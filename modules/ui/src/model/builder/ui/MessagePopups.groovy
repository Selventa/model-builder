package model.builder.ui

import static javax.swing.JOptionPane.*
import groovy.swing.SwingBuilder

class MessagePopups {

    static void errorConnectionAccess(String host, String email, String pass) {
        def swing = new SwingBuilder()
        String msg = "The email or password is incorrect.  Please verify\n" +
                     "the entered information:\n" +
                     "\n\tHost: $host\n\tEmail: $email\n\tPassword: $pass"
        swing.optionPane(message: msg, messageType: ERROR_MESSAGE).
                createDialog(null, 'Connection Error').setVisible(true)
    }

    /**
     * Static access only.
     */
    private MessagePopups() {}
}