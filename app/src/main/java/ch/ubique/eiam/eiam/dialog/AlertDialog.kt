package ch.ubique.eiam.eiam.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.ubique.eiam.R
import ch.ubique.eiam.eiam.PopupModel


@Composable
fun EiamAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    popupModel: PopupModel,
    err: Throwable? = null
) {
    val dialogTitle = popupModel.dialogTitleId?.let {
        stringResource(id = it)
    } ?: run {
        stringResource(id = R.string.error_title)
    }
    val dialogText = if(err != null) {
        stringResource(id = R.string.error_title, err)
    } else {
        popupModel.dialogTextId?.let {
            stringResource(id = it)
        } ?: run {
            popupModel.dialogText ?: ""
        }
    }

    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Ok")
            }
        }
    )
}