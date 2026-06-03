package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.pow

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val departmentsListType = Types.newParameterizedType(List::class.java, DepartmentState::class.java)
    private val departmentsAdapter = moshi.adapter<List<DepartmentState>>(departmentsListType)

    private val activeUpgradesListType = Types.newParameterizedType(List::class.java, ActiveUpgradeState::class.java)
    private val activeUpgradesAdapter = moshi.adapter<List<ActiveUpgradeState>>(activeUpgradesListType)

    private val marketEventAdapter = moshi.adapter(MarketEventState::class.java)

    // UI States
    private val _sector = MutableStateFlow("")
    val sector: StateFlow<String> = _sector.asStateFlow()

    private val _cash = MutableStateFlow(500.0)
    val cash: StateFlow<Double> = _cash.asStateFlow()

    private val _totalTaps = MutableStateFlow(0)
    val totalTaps: StateFlow<Int> = _totalTaps.asStateFlow()

    private val _loanAmount = MutableStateFlow(0.0)
    val loanAmount: StateFlow<Double> = _loanAmount.asStateFlow()

    private val _departments = MutableStateFlow<List<DepartmentState>>(emptyList())
    val departments: StateFlow<List<DepartmentState>> = _departments.asStateFlow()

    private val _upgrades = MutableStateFlow<List<ActiveUpgradeState>>(emptyList())
    val upgrades: StateFlow<List<ActiveUpgradeState>> = _upgrades.asStateFlow()

    private val _currentEvent = MutableStateFlow<MarketEventState?>(null)
    val currentEvent: StateFlow<MarketEventState?> = _currentEvent.asStateFlow()

    // Offline progress state
    private val _offlineEarnings = MutableStateFlow<Double?>(null)
    val offlineEarnings: StateFlow<Double?> = _offlineEarnings.asStateFlow()

    private val _offlineDurationSeconds = MutableStateFlow<Long>(0L)
    val offlineDurationSeconds: StateFlow<Long> = _offlineDurationSeconds.asStateFlow()

    // Screen states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Simulated Ad state
    private val _activeAdUpgradeId = MutableStateFlow<String?>(null)
    val activeAdUpgradeId: StateFlow<String?> = _activeAdUpgradeId.asStateFlow()

    private val _adTimerSeconds = MutableStateFlow(5)
    val adTimerSeconds: StateFlow<Int> = _adTimerSeconds.asStateFlow()

    private val _isAdRewardReady = MutableStateFlow(false)
    val isAdRewardReady: StateFlow<Boolean> = _isAdRewardReady.asStateFlow()

    private val _adCompany = MutableStateFlow("Sponsor Corp")
    val adCompany: StateFlow<String> = _adCompany.asStateFlow()

    private var gameLoopJob: Job? = null
    private var adTimerJob: Job? = null

    init {
        loadGameState()
    }

    private fun loadGameState() = viewModelScope.launch {
        _isLoading.value = true
        val progress = repository.getProgress()
        if (progress != null) {
            _sector.value = progress.sector
            _cash.value = progress.cash
            _totalTaps.value = progress.totalTaps
            _loanAmount.value = progress.loanAmount

            if (progress.sector.isNotEmpty()) {
                // Deserialize existing progress
                _departments.value = deserializeDepartments(progress.departmentsJson)
                _upgrades.value = deserializeUpgrades(progress.activeUpgradesJson)
                _currentEvent.value = deserializeEvent(progress.activeEventJson)

                // Process time passed offline
                calculateOfflineProgress(progress.lastActiveTimestamp)
            }
        }
        _isLoading.value = false
        startGameLoop()
    }

    private fun calculateOfflineProgress(lastActiveTime: Long) {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastActiveTime) / 1000
        if (elapsedSeconds < 5) return // Ignore very short interruptions

        _offlineDurationSeconds.value = elapsedSeconds

        // 1. Process time-based upgrades completed offline
        var upgradedCount = 0
        val currentUpgrades = _upgrades.value.map { upgrade ->
            if (upgrade.hasStarted && !upgrade.isFinished && upgrade.finishTimestamp <= currentTime) {
                upgradedCount++
                upgrade.copy(isFinished = true, hasStarted = false)
            } else {
                upgrade
            }
        }
        _upgrades.value = currentUpgrades

        // Apply finished upgrades to departments immediately
        applyFinishedUpgradesToDepartments()

        // 2. Compute passive revenue generated during offline period
        val passiveRevenueRate = getPassiveIncomePerSecond()
        val totalPassiveIncome = passiveRevenueRate * elapsedSeconds
        
        // Apply offline limit of 12 hours (43,200 seconds)
        val cappedIncome = if (elapsedSeconds > 43200) {
            passiveRevenueRate * 43200
        } else {
            totalPassiveIncome
        }

        if (cappedIncome > 0) {
            // Repay part of loan automatically if active (15% auto-deduction)
            var actualOfflineEarningAdded = cappedIncome
            if (_loanAmount.value > 0.0) {
                val autoDeduction = cappedIncome * 0.15
                val deducAmount = minOf(autoDeduction, _loanAmount.value)
                _loanAmount.value -= deducAmount
                actualOfflineEarningAdded -= deducAmount
            }
            _cash.value += actualOfflineEarningAdded
            _offlineEarnings.value = actualOfflineEarningAdded
        }
    }

    fun dismissOfflineDialog() {
        _offlineEarnings.value = null
        _offlineDurationSeconds.value = 0L
    }

    // Initialize Game Data based on chosen startup sector
    fun chooseSector(selectedSector: String) = viewModelScope.launch {
        _sector.value = selectedSector
        _cash.value = 500.0
        _totalTaps.value = 0
        _loanAmount.value = 0.0

        val (initialDepts, initialUpgrades) = generateInitialSectorData(selectedSector)
        _departments.value = initialDepts
        _upgrades.value = initialUpgrades
        _currentEvent.value = null

        saveCurrentState()
    }

    private fun generateInitialSectorData(sect: String): Pair<List<DepartmentState>, List<ActiveUpgradeState>> {
        val depts = when (sect) {
            "TECH" -> listOf(
                DepartmentState("DEV", "Software Development", 1, 80.0, 1.15, 2.0, 1.12, false, 250.0, "Codebot AI"),
                DepartmentState("CLOUD", "Cloud Scaling", 0, 500.0, 1.25, 15.0, 1.15, false, 1500.0, "Sarah Cloud Ops"),
                DepartmentState("AI", "Research & Artificial Intelligence", 0, 3000.0, 1.35, 90.0, 1.18, false, 8000.0, "Quantum Core AI")
            )
            "RETAIL" -> listOf(
                DepartmentState("MART", "Local Corner Shop", 1, 50.0, 1.12, 1.20, 1.10, false, 200.0, "Sponsor Max"),
                DepartmentState("SUPER", "Supermarket Franchise", 0, 400.0, 1.22, 10.0, 1.13, false, 1200.0, "Store Exec Jerry"),
                DepartmentState("MALL", "Mega Shopping Plaza", 0, 2500.0, 1.32, 75.0, 1.16, false, 6000.0, "COO Ken Mall")
            )
            else -> listOf( // "LOGISTICS"
                DepartmentState("VAN", "Last-Mile Delivery Vans", 1, 100.0, 1.18, 3.0, 1.14, false, 300.0, "Tom Dispatcher"),
                DepartmentState("TRUCK", "Cross-State Semis", 0, 650.0, 1.28, 18.0, 1.16, false, 1800.0, "Clara Fleet Lead"),
                DepartmentState("CARGO", "Global Freight Line", 0, 4000.0, 1.38, 110.0, 1.20, false, 9000.0, "Iris AI Route Op")
            )
        }

        // Realistic Time-Based Upgrades list matching exactly Tier 1 (1-10m), Tier 2 (40-80m), Tier 3 (1-2hr)
        val upgrades = listOf(
            // Tier 1 Upgrades
            ActiveUpgradeState("UP_T1_1", "Fiber Optic Line / Store Makeover / GPS Fleet Refit", "ALL", 1200.0, 1, 1, 120, 0), // 2 Minutes
            ActiveUpgradeState("UP_T1_2", "Guerrilla Viral Ad Campaign", "ALL", 3500.0, 1, 1, 300, 0),                       // 5 Minutes
            ActiveUpgradeState("UP_T1_3", "High-performance Server Node / Staff Training Program", "ALL", 6000.0, 1, 1, 480, 0), // 8 Minutes

            // Tier 2 Upgrades (Mid Game)
            ActiveUpgradeState("UP_T2_1", "Regional Hub / Logistics Distribution Facility", "ALL", 25000.0, 1, 1, 2400, 0),     // 40 Minutes
            ActiveUpgradeState("UP_T2_2", "National Prime TV Ad Stream", "ALL", 45000.0, 1, 1, 3600, 0),                         // 60 Minutes (1 Hour)
            ActiveUpgradeState("UP_T2_3", "Dynamic Neural Network Route Optimization", "ALL", 75000.0, 1, 1, 4800, 0),           // 80 Minutes

            // Tier 3 Upgrades (Late Game)
            ActiveUpgradeState("UP_T3_1", "Smart Autonomous Drones Launch Center", "ALL", 250000.0, 1, 1, 5400, 0),              // 90 Minutes (1.5 Hours)
            ActiveUpgradeState("UP_T3_2", "Global Commercial Brand Acquisition", "ALL", 500000.0, 1, 1, 7200, 0),                 // 120 Minutes (2 Hours)
            ActiveUpgradeState("UP_T3_3", "Fully Automated Quantum Autonomous Hubs", "ALL", 900000.0, 1, 1, 7200, 0)             // 120 Minutes (2 Hours)
        )

        return Pair(depts, upgrades)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(1000)

                if (_sector.value.isEmpty()) continue

                // 1. Process active timers
                var anyTimeUpgradeFinishing = false
                val currentTime = System.currentTimeMillis()
                _upgrades.value = _upgrades.value.map { upgrade ->
                    if (upgrade.hasStarted && !upgrade.isFinished) {
                        if (currentTime >= upgrade.finishTimestamp) {
                            anyTimeUpgradeFinishing = true
                            upgrade.copy(isFinished = true, hasStarted = false)
                        } else {
                            upgrade
                        }
                    } else {
                        upgrade
                    }
                }

                if (anyTimeUpgradeFinishing) {
                    applyFinishedUpgradesToDepartments()
                }

                // 2. Clear market event if time finished
                _currentEvent.value?.let { evt ->
                    if (currentTime >= evt.finishTimestamp) {
                        _currentEvent.value = null
                    }
                }

                // 3. Chance of triggering random market event
                if (_currentEvent.value == null && Math.random() < 0.015) {
                    triggerRandomMarketEvent()
                }

                // 4. Collect passive income
                val netIncomePerSec = getPassiveIncomePerSecond()
                if (netIncomePerSec > 0) {
                    var remainingNetIncome = netIncomePerSec
                    // Loan Auto Repay (15% dedication rate)
                    if (_loanAmount.value > 0.0) {
                        val repayment = netIncomePerSec * 0.15
                        val actualRepayment = minOf(repayment, _loanAmount.value)
                        _loanAmount.value -= actualRepayment
                        remainingNetIncome -= actualRepayment
                    }
                    _cash.value += remainingNetIncome
                }

                // Save game state roughly every 5 ticks
                if (System.currentTimeMillis() % 5 == 0L) {
                    saveCurrentState()
                }
            }
        }
    }

    // Apply the active upgrade benefits to departments
    private fun applyFinishedUpgradesToDepartments() {
        val finishedUpgrades = _upgrades.value.filter { it.isFinished }
        _departments.update { depts ->
            depts.map { dept ->
                var updatedDept = dept
                finishedUpgrades.forEach { upgrade ->
                    // Make upgrading apply general overall boosts or specific booster multipliers
                    when (upgrade.id) {
                        "UP_T1_1" -> updatedDept = updatedDept.copy(revenueMultiplier = updatedDept.revenueMultiplier * 1.15)
                        "UP_T1_2" -> updatedDept = updatedDept.copy(baseRevenue = updatedDept.baseRevenue * 1.30)
                        "UP_T1_3" -> updatedDept = updatedDept.copy(costMultiplier = maxOf(1.05, updatedDept.costMultiplier * 0.92))
                        "UP_T2_1" -> updatedDept = updatedDept.copy(baseRevenue = updatedDept.baseRevenue * 2.0)
                        "UP_T2_2" -> updatedDept = updatedDept.copy(revenueMultiplier = updatedDept.revenueMultiplier * 1.50)
                        "UP_T2_3" -> updatedDept = updatedDept.copy(costMultiplier = maxOf(1.05, updatedDept.costMultiplier * 0.85))
                        "UP_T3_1" -> updatedDept = updatedDept.copy(baseRevenue = updatedDept.baseRevenue * 3.5)
                        "UP_T3_2" -> updatedDept = updatedDept.copy(revenueMultiplier = updatedDept.revenueMultiplier * 2.20)
                        "UP_T3_3" -> updatedDept = updatedDept.copy(baseRevenue = updatedDept.baseRevenue * 5.0)
                    }
                }
                updatedDept
            }
        }
    }

    // Formulas for passive earnings and tap earnings
    fun getPassiveIncomePerSecond(): Double {
        var total = 0.0
        _departments.value.forEach { dept ->
            if (dept.level > 0 && dept.isAutomated) {
                val base = dept.baseRevenue * (dept.revenueMultiplier.pow(dept.level - 1))
                total += base
            }
        }

        // Apply Event multipliers
        _currentEvent.value?.let { event ->
            total *= event.revenueMultiplier
        }
        return total
    }

    fun getTapIncomeAmount(): Double {
        // Tap income increases with the highest level department we own, and general active upgrades
        val maxLevel = _departments.value.maxOfOrNull { it.level } ?: 1
        var tapAmount = 1.0 + (maxLevel * 1.25)
        
        // Multipliers from tier 1/2/3 upgrades completed
        val completeCount = _upgrades.value.count { it.isFinished }
        tapAmount *= (1.0 + completeCount * 0.5)

        // Event Multiplier
        _currentEvent.value?.let { event ->
            tapAmount *= event.revenueMultiplier
        }
        return tapAmount
    }

    // User Taps To Generate Cash
    fun tapToEarn() {
        val tapAmount = getTapIncomeAmount()
        _totalTaps.value += 1
        var addedCash = tapAmount

        // 15% loan auto repayment on taps as well
        if (_loanAmount.value > 0.0) {
            val repayment = tapAmount * 0.15
            val actualRepayment = minOf(repayment, _loanAmount.value)
            _loanAmount.value -= actualRepayment
            addedCash -= actualRepayment
        }

        _cash.value += addedCash
        
        // Immediate tiny save on clicks to keep state safe
        if (_totalTaps.value % 25 == 0) {
            saveCurrentState()
        }
    }

    // Buy department upgrade instantly (raises levels)
    fun upgradeDepartment(id: String) {
        val dept = _departments.value.find { it.id == id } ?: return
        val currentLevel = dept.level
        val upgradeCost = dept.baseCost * (dept.costMultiplier.pow(currentLevel))

        if (_cash.value >= upgradeCost) {
            _cash.value -= upgradeCost
            _departments.update { list ->
                list.map { d ->
                    if (d.id == id) {
                        d.copy(level = d.level + 1)
                    } else {
                        d
                    }
                }
            }
            saveCurrentState()
        }
    }

    // Buy (Unlock) a Locked Department
    fun unlockDepartment(id: String) {
        val dept = _departments.value.find { it.id == id } ?: return
        if (dept.level == 0 && _cash.value >= dept.baseCost) {
            _cash.value -= dept.baseCost
            _departments.update { list ->
                list.map { d ->
                    if (d.id == id) {
                        d.copy(level = 1)
                    } else {
                        d
                    }
                }
            }
            saveCurrentState()
        }
    }

    // Hire Department Manager to Automate Revenue Generation
    fun hireManagerForDepartment(id: String) {
        val dept = _departments.value.find { it.id == id } ?: return
        if (!dept.isAutomated && dept.level > 0 && _cash.value >= dept.managerCost) {
            _cash.value -= dept.managerCost
            _departments.update { list ->
                list.map { d ->
                    if (d.id == id) {
                        d.copy(isAutomated = true)
                    } else {
                        d
                    }
                }
            }
            saveCurrentState()
        }
    }

    // Start a long-running time-based upgrade
    fun startExpansionUpgrade(id: String) {
        val upgrade = _upgrades.value.find { it.id == id } ?: return
        if (!upgrade.hasStarted && !upgrade.isFinished && _cash.value >= upgrade.cost) {
            // Deduct cost and save
            _cash.value -= upgrade.cost
            val currentTime = System.currentTimeMillis()
            val finalFinishTime = currentTime + (upgrade.totalDurationSeconds * 1000)

            _upgrades.update { list ->
                list.map { u ->
                    if (u.id == id) {
                        u.copy(hasStarted = true, finishTimestamp = finalFinishTime)
                    } else {
                        u
                    }
                }
            }
            saveCurrentState()
        }
    }

    // Trigger Mock Ad Video View & Skip 5 Minutes
    fun triggerWatchAd(upgradeId: String) {
        val adSponsors = listOf(
            "Eco-Logistics AI Fuels", "ByteServer SSD Web", "Nvidia Cloud Devchips",
            "SiriTech Solutions", "Apex Retail Cargo Group", "Quantum Auto Route Corp"
        )
        _activeAdUpgradeId.value = upgradeId
        _adTimerSeconds.value = 5
        _isAdRewardReady.value = false
        _adCompany.value = adSponsors.random()

        adTimerJob?.cancel()
        adTimerJob = viewModelScope.launch {
            while (_adTimerSeconds.value > 0) {
                delay(1000)
                _adTimerSeconds.value -= 1
            }
            _isAdRewardReady.value = true
        }
    }

    fun claimAdTimeSkip() {
        val upgradeId = _activeAdUpgradeId.value ?: return
        if (_isAdRewardReady.value) {
            // Skip 5 minutes (300 seconds) on specified upgrade
            _upgrades.update { list ->
                list.map { u ->
                    if (u.id == upgradeId && u.hasStarted && !u.isFinished) {
                        val revisedFinish = u.finishTimestamp - (300 * 1000)
                        u.copy(finishTimestamp = maxOf(System.currentTimeMillis(), revisedFinish))
                    } else {
                        u
                    }
                }
            }

            // Immediately check and apply updates when timeskip completes upgrades
            var anyTimeUpgradeFinishing = false
            val currentTime = System.currentTimeMillis()
            _upgrades.value = _upgrades.value.map { upgrade ->
                if (upgrade.hasStarted && !upgrade.isFinished && currentTime >= upgrade.finishTimestamp) {
                    anyTimeUpgradeFinishing = true
                    upgrade.copy(isFinished = true, hasStarted = false)
                } else {
                    upgrade
                }
            }
            if (anyTimeUpgradeFinishing) {
                applyFinishedUpgradesToDepartments()
            }

            // Clear ad overlay
            _activeAdUpgradeId.value = null
            _isAdRewardReady.value = false
            saveCurrentState()
        }
    }

    fun dismissAd() {
        adTimerJob?.cancel()
        _activeAdUpgradeId.value = null
        _isAdRewardReady.value = false
    }

    // Financial Loan Simulation
    fun takeBankLoan(amount: Double) {
        // Can take loans, maximum outstanding is calculated
        _loanAmount.value += amount
        _cash.value += amount
        saveCurrentState()
    }

    fun repayBankLoanAmount(amount: Double) {
        val repayAmount = minOf(amount, _loanAmount.value, _cash.value)
        if (repayAmount > 0) {
            _cash.value -= repayAmount
            _loanAmount.value -= repayAmount
            saveCurrentState()
        }
    }

    // Random Event Simulation triggers
    fun triggerRandomMarketEvent() {
        val events = when ((0..2).random()) {
            0 -> MarketEventState(
                "BOOM",
                "MARKET IN RALLY!",
                "Consumer interest surged! Double Multiplier (2x) applied to All departments!",
                2.0,
                System.currentTimeMillis() + 60_000,
                "BOOM"
            )
            1 -> MarketEventState(
                "CRASH",
                "MARKET CRASH!",
                "Stricter regulations, supply cuts! Global Earnings reduced by 50% for 60 seconds.",
                0.5,
                System.currentTimeMillis() + 60_000,
                "CRASH"
            )
            else -> MarketEventState(
                "INFLATION",
                "TECH & RETAIL STIMULUS!",
                "Corporate grants active! Global Earnings increased by 1.5x.",
                1.5,
                System.currentTimeMillis() + 60_000,
                "BOOM"
            )
        }
        _currentEvent.value = events
    }

    // Database Serializers / Deserializers
    private fun deserializeDepartments(json: String): List<DepartmentState> {
        return try {
            if (json.isEmpty()) emptyList() else departmentsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeDepartments(list: List<DepartmentState>): String {
        return try {
            departmentsAdapter.toJson(list)
        } catch (e: Exception) {
            ""
        }
    }

    private fun deserializeUpgrades(json: String): List<ActiveUpgradeState> {
        return try {
            if (json.isEmpty()) emptyList() else activeUpgradesAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeUpgrades(list: List<ActiveUpgradeState>): String {
        return try {
            activeUpgradesAdapter.toJson(list)
        } catch (e: Exception) {
            ""
        }
    }

    private fun deserializeEvent(json: String): MarketEventState? {
        return try {
            if (json.isEmpty()) null else marketEventAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeEvent(event: MarketEventState?): String {
        return try {
            if (event == null) "" else marketEventAdapter.toJson(event)
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveCurrentState() = viewModelScope.launch {
        val state = GameProgress(
            id = 1,
            sector = _sector.value,
            cash = _cash.value,
            totalTaps = _totalTaps.value,
            loanAmount = _loanAmount.value,
            lastActiveTimestamp = System.currentTimeMillis(),
            departmentsJson = serializeDepartments(_departments.value),
            activeUpgradesJson = serializeUpgrades(_upgrades.value),
            activeEventJson = serializeEvent(_currentEvent.value)
        )
        repository.saveProgress(state)
    }

    fun resetGame() = viewModelScope.launch {
        _isLoading.value = true
        repository.clearProgress()
        _sector.value = ""
        _cash.value = 500.0
        _totalTaps.value = 0
        _loanAmount.value = 0.0
        _departments.value = emptyList()
        _upgrades.value = emptyList()
        _currentEvent.value = null
        _offlineEarnings.value = null
        _isLoading.value = false
    }

    // Helper formatter for large Numbers to display properly on the game board
    fun formatMoney(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format("$%.2fB", amount / 1_000_000_000.0)
            amount >= 1_000_000 -> String.format("$%.2fM", amount / 1_000_000.0)
            amount >= 1_000 -> String.format("$%.1fK", amount / 1_000.0)
            else -> String.format("$%.2f", amount)
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        adTimerJob?.cancel()
    }
}
