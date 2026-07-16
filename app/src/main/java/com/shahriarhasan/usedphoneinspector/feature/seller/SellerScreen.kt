package com.shahriarhasan.usedphoneinspector.feature.seller

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.shahriarhasan.usedphoneinspector.R
import com.shahriarhasan.usedphoneinspector.core.design.InformationCard
import com.shahriarhasan.usedphoneinspector.core.model.InspectionProfile
import com.shahriarhasan.usedphoneinspector.core.model.SellerDetails

@Composable
fun SellerScreen(
    profile: InspectionProfile,
    seller: SellerDetails,
    onChanged: (SellerDetails) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            InformationCard(
                stringResource(R.string.seller_privacy_title),
                stringResource(R.string.seller_privacy_body),
            )
        }
        item { SellerField(R.string.seller_name, seller.name) { onChanged(seller.copy(name = it)) } }
        item { SellerField(R.string.phone_number, seller.phone, KeyboardType.Phone) { onChanged(seller.copy(phone = it)) } }
        item { SellerField(R.string.alternate_phone, seller.alternatePhone, KeyboardType.Phone) { onChanged(seller.copy(alternatePhone = it)) } }
        item { SellerField(R.string.email, seller.email, KeyboardType.Email) { onChanged(seller.copy(email = it)) } }
        item { SellerField(R.string.business_name, seller.businessName) { onChanged(seller.copy(businessName = it)) } }
        item { SellerField(R.string.location, seller.location) { onChanged(seller.copy(location = it)) } }
        item { SellerField(R.string.address, seller.address, singleLine = false) { onChanged(seller.copy(address = it)) } }
        item { SellerField(R.string.payment_method, seller.paymentMethod) { onChanged(seller.copy(paymentMethod = it)) } }
        item { SellerField(R.string.warranty, seller.warranty) { onChanged(seller.copy(warranty = it)) } }
        item { SellerField(R.string.purchase_date, seller.purchaseDate) { onChanged(seller.copy(purchaseDate = it)) } }
        item { SellerField(R.string.seller_notes, seller.sellerNotes, singleLine = false) { onChanged(seller.copy(sellerNotes = it)) } }
        item { SellerField(R.string.buyer_notes, seller.buyerNotes, singleLine = false) { onChanged(seller.copy(buyerNotes = it)) } }
        if (profile == InspectionProfile.REPAIR_SHOP_INTAKE) {
            item { Text(stringResource(R.string.repair_intake_details)) }
            item {
                SellerField(R.string.customer_problem, seller.repairDetailsJson, singleLine = false) {
                    onChanged(seller.copy(repairDetailsJson = it))
                }
            }
            item { Text(stringResource(R.string.device_lock_state)) }
            item { Text(stringResource(R.string.accessories_received)) }
            item { Text(stringResource(R.string.sim_received)) }
            item { Text(stringResource(R.string.memory_card_received)) }
            item { Text(stringResource(R.string.existing_damage)) }
            item { Text(stringResource(R.string.data_loss_acknowledged)) }
            item { Text(stringResource(R.string.expected_delivery)) }
            item { Text(stringResource(R.string.estimated_repair_cost)) }
            item { Text(stringResource(R.string.technician_notes)) }
        }
    }
}

@Composable
private fun SellerField(
    label: Int,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(2_000)) },
        label = { Text(stringResource(label)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

