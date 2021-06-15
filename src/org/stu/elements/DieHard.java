package org.stu.elements;

/**
 * The type Die hard.
 *
 * @author Alireza Jabbari
 */
public class DieHard extends Citizen{
    private int remainInquiry = 2;
    private boolean hasSaved = true;

    /**
     * Use inquiry.
     */
    public void useInquiry(){
        remainInquiry--;
    }

    /**
     * Have inquiry boolean.
     *
     * @return true if have inquiry, otherwise false
     */
    public boolean haveInquiry(){
        return remainInquiry > 0;
    }

    /**
     * Use shield.
     */
    public void useShield(){
        hasSaved = false;
    }

    /**
     * Have shield boolean.
     *
     * @return true if have shield, otherwise false
     */
    public boolean haveShield(){
        return hasSaved;
    }
}
