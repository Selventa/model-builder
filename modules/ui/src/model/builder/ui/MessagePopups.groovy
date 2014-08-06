package model.builder.ui

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static javax.swing.JOptionPane.*
import groovy.swing.SwingBuilder

class MessagePopups {

    private static final Logger userMessages = LoggerFactory.getLogger('CyUserMessages');

    static void errorAccessNotSet() {
        def swing = new SwingBuilder()
        String msg = "An SDP server has not been configured or set as default.\n\n" +
                     "Open Apps -> SDP -> Configure (shortcut: Ctrl + Alt + O)"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    static void errorConnectionAccess(String host, String email, String pass) {
        def swing = new SwingBuilder()
        String msg = "The email or password is incorrect.  Please verify\n" +
                     "the entered information:\n" +
                     "\n\tHost: $host\n\tEmail: $email\n\tPassword: $pass"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    static int modelConflict(String model, Integer oldRev, String newRevWho,
                             String newRevComment, String newRevWhen) {
        String msg = "Save failed for \"$model\" because this version is out of date (revision $oldRev).\n\n" +
                "The latest saved revision is:\n" +
                "\n\tUser: $newRevWho\n\tComment: $newRevComment" +
                "\n\tDate: $newRevWhen" +
                "\n\nYou will have to manually apply your changes to the latest saved revision.\n" +
                "Then you may save the merged changes as a new revision." +
                "\n\nWould you like to open the latest revision?"
        showConfirmDialog(null, msg, "Revision conflict", YES_NO_OPTION);
    }

    static void successMessage(String msg) {
        userMessages.info(msg)
    }

    static void errorMessage(String msg) {
        userMessages.error(msg)
    }

    /**
     * Static access only.
     */
    private MessagePopups() {}
}
