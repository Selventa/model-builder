package model.builder.ui

import javax.swing.ImageIcon

class Util {

    static ImageIcon icon(String path, String desc) {
        def url = Util.class.getResource(path)
        url ? new ImageIcon(url, desc) : null
    }
}
