package model.builder.ui

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static javax.swing.JOptionPane.*
import groovy.swing.SwingBuilder

class MessagePopups {

    private static final Logger userMessages = LoggerFactory.getLogger('CyUserMessages');

    static void errorConnectionAccess(String host, String email, String pass) {
        def swing = new SwingBuilder()
        String msg = "The email or password is incorrect.  Please verify\n" +
                     "the entered information:\n" +
                     "\n\tHost: $host\n\tEmail: $email\n\tPassword: $pass"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    static void successMessage(String msg) {
        userMessages.info(msg)
    }

    /**
     * Static access only.
     */
    private MessagePopups() {}
}
