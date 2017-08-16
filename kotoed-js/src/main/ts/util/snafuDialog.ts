import * as $ from "jquery"
import "bootstrap"

export const DO_NOT_BOTHER_ME_AGAIN = "do-not-bother-me-again";

function initSnafuDialog(): JQuery<Node> | undefined {
    let dialog = $("#snafu-dialog");
    let reloadButton = $("#snafu-dialog-reload");
    let stayButton = $("#snafu-dialog-stay");
    let goodLuckDialog = $("#snafu-good-luck-dialog");
    let goodLuckDialogOkay = $("#snafu-good-luck-dialog-okay");

    if (dialog.length === 0 ||
        reloadButton.length === 0 ||
        stayButton.length === 0 ||
        goodLuckDialog.length === 0 ||
        goodLuckDialogOkay.length === 0
    ) {
        console.warn("Error dialog is not present in DOM");
        return undefined;
    }

    dialog.data(DO_NOT_BOTHER_ME_AGAIN, false);

    reloadButton.on('click' , () => {
        window.location.reload(true);
    });

    stayButton.on('click' , () => {
        dialog.modal('hide');
        dialog.data(DO_NOT_BOTHER_ME_AGAIN, true);
        goodLuckDialog.modal('show');
    });

    goodLuckDialogOkay.on('click' , () => {
        goodLuckDialog.modal('hide');
    });

    return dialog

}

const dialog = initSnafuDialog();

export default function snafuDialog() {
    if (!dialog)
        alert("Something went wrong. Try reloading this page.");
    else if (!dialog.data(DO_NOT_BOTHER_ME_AGAIN))
        dialog.modal({
            backdrop: 'static',
            keyboard: false
        })
}
