package com.shahriarhasan.usedphoneinspector.feature.inspection

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shahriarhasan.usedphoneinspector.core.database.InspectionRepository
import com.shahriarhasan.usedphoneinspector.core.datastore.SettingsRepository
import com.shahriarhasan.usedphoneinspector.core.model.DeviceCategory
import com.shahriarhasan.usedphoneinspector.core.model.InspectionDraft
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SetupEvent {
    data class BrandChanged(val value: String) : SetupEvent
    data class ModelChanged(val value: String) : SetupEvent
    data class ColorChanged(val value: String) : SetupEvent
    data class StorageChanged(val value: String) : SetupEvent
    data class RamChanged(val value: String) : SetupEvent
    data class AskingPriceChanged(val value: String) : SetupEvent
    data class FinalPriceChanged(val value: String) : SetupEvent
    data class CurrencyChanged(val value: String) : SetupEvent
    data class SourceChanged(val value: String) : SetupEvent
    data class SerialChanged(val value: String) : SetupEvent
    data class Imei1Changed(val value: String) : SetupEvent
    data class Imei2Changed(val value: String) : SetupEvent
    data class NotesChanged(val value: String) : SetupEvent
    data class ProfileChanged(val value: InspectionProfile) : SetupEvent
    data class CategoryChanged(val value: DeviceCategory) : SetupEvent
    data object Submit : SetupEvent
}

data class SetupUiState(
    val draft: InspectionDraft = InspectionDraft(brand = Build.BRAND, model = Build.MODEL),
    val isSaving: Boolean = false,
    val showRequiredError: Boolean = false,
)

@HiltViewModel
class InspectionSetupViewModel @Inject constructor(
    private val inspectionRepository: InspectionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = mutableState
    private val navigationChannel = Channel<String>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            mutableState.value = mutableState.value.copy(
                draft = mutableState.value.draft.copy(
                    profile = settings.defaultProfile,
                    deviceCategory = if (settings.defaultProfile == InspectionProfile.USED_TABLET) {
                        DeviceCategory.TABLET
                    } else DeviceCategory.PHONE,
                    currency = settings.defaultCurrency,
                ),
            )
        }
    }

    fun onEvent(event: SetupEvent) {
        val current = mutableState.value
        val draft = current.draft
        mutableState.value = when (event) {
            is SetupEvent.BrandChanged -> current.copy(draft = draft.copy(brand = event.value))
            is SetupEvent.ModelChanged -> current.copy(draft = draft.copy(model = event.value))
            is SetupEvent.ColorChanged -> current.copy(draft = draft.copy(color = event.value))
            is SetupEvent.StorageChanged -> current.copy(draft = draft.copy(storageVariant = event.value))
            is SetupEvent.RamChanged -> current.copy(draft = draft.copy(ramVariant = event.value))
            is SetupEvent.AskingPriceChanged -> current.copy(draft = draft.copy(askingPrice = event.value))
            is SetupEvent.FinalPriceChanged -> current.copy(draft = draft.copy(finalPrice = event.value))
            is SetupEvent.CurrencyChanged -> current.copy(draft = draft.copy(currency = event.value))
            is SetupEvent.SourceChanged -> current.copy(draft = draft.copy(purchaseSource = event.value))
            is SetupEvent.SerialChanged -> current.copy(draft = draft.copy(serialNumber = event.value))
            is SetupEvent.Imei1Changed -> current.copy(draft = draft.copy(imei1 = event.value.filter(Char::isDigit).take(20)))
            is SetupEvent.Imei2Changed -> current.copy(draft = draft.copy(imei2 = event.value.filter(Char::isDigit).take(20)))
            is SetupEvent.NotesChanged -> current.copy(draft = draft.copy(notes = event.value))
            is SetupEvent.ProfileChanged -> current.copy(
                draft = draft.copy(
                    profile = event.value,
                    deviceCategory = if (event.value == InspectionProfile.USED_TABLET) DeviceCategory.TABLET else draft.deviceCategory,
                ),
            )
            is SetupEvent.CategoryChanged -> current.copy(draft = draft.copy(deviceCategory = event.value))
            SetupEvent.Submit -> current
        }
        if (event == SetupEvent.Submit) submit()
    }

    private fun submit() {
        val draft = mutableState.value.draft
        if (draft.brand.isBlank() || draft.model.isBlank()) {
            mutableState.value = mutableState.value.copy(showRequiredError = true)
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSaving = true, showRequiredError = false)
            val id = inspectionRepository.create(draft)
            navigationChannel.send(id)
            mutableState.value = mutableState.value.copy(isSaving = false)
        }
    }
}

