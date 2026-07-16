package com.shahriarhasan.usedphoneinspector.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.database.InspectionWithDetails
import com.shahriarhasan.usedphoneinspector.core.model.ConditionGrade
import com.shahriarhasan.usedphoneinspector.core.model.HistoryFilter
import com.shahriarhasan.usedphoneinspector.core.model.HistorySort
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.InspectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: InspectionRepository,
) : ViewModel() {
    private val mutableFilter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = mutableFilter
    val results: StateFlow<List<InspectionWithDetails>> = mutableFilter
        .flatMapLatest(repository::search)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val navigationChannel = Channel<String>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()

    fun setQuery(value: String) { mutableFilter.value = mutableFilter.value.copy(query = value.take(100)) }
    fun setProfile(value: InspectionProfile?) { mutableFilter.value = mutableFilter.value.copy(profile = value) }
    fun setGrade(value: ConditionGrade?) { mutableFilter.value = mutableFilter.value.copy(grade = value) }
    fun setStatus(value: InspectionStatus?) { mutableFilter.value = mutableFilter.value.copy(status = value) }
    fun setSort(value: HistorySort) { mutableFilter.value = mutableFilter.value.copy(sort = value) }
    fun setDates(start: Long?, end: Long?) { mutableFilter.value = mutableFilter.value.copy(startDate = start, endDate = end) }
    fun delete(id: String) { viewModelScope.launch { repository.delete(id) } }
    fun duplicate(id: String) {
        viewModelScope.launch { navigationChannel.send(repository.duplicate(id)) }
    }
}

