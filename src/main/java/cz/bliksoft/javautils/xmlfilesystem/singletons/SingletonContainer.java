/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.bliksoft.javautils.xmlfilesystem.singletons;

import cz.bliksoft.javautils.xmlfilesystem.FileObject;

/**
 * kontejner pro uchovávání singletonu a jeho definičního záznamu ve filesystému
 * @author hroch
 */
public class SingletonContainer {

    protected Object value;
    protected FileObject file;

    public SingletonContainer(Object value, FileObject file) {
        this.value = value;
        this.file = file;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SingletonContainer) {
            return file == (((SingletonContainer) obj).file);
        } else if (obj instanceof FileObject) {
            return file == obj;
        } else {
            return false;
        }
    }

    public Object getValue() {
        return value;
    }
}
