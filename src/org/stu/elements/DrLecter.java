package org.stu.elements;

/**
 * The type Dr lecter.
 *
 * @author Alireza Jabbari
 */
public class DrLecter extends Mafia{
    private boolean savedHimself = false;

    /**
     * Is saved himself boolean.
     *
     * @return true if saved himself, otherwise false
     */
    public boolean isSavedHimself(){
        return savedHimself;
    }

    /**
     * Set saved himself.
     *
     * @param savedHimself saved himself boolean
     */
    public void setSavedHimself(boolean savedHimself){
        this.savedHimself = savedHimself;
    }
}
