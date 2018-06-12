package com.apoorv.bibliotheca;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.InputStream;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.core.StringStartsWith.startsWith;

@RunWith(AndroidJUnit4.class)
public class UnitTest {
    @Test
    public void testParcer(){
        Parcer pr = new Parcer();
        String text = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<FictionBook xmlns=\"http://www.gribuser.ru/xml/fictionbook/2.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n" +
                "    <description>\n" +
                "        <title-info>\n" +
                "            <genre>antique</genre>\n" +
                "                <author><first-name></first-name><last-name>Unknown</last-name></author>\n" +
                "            <book-title>023c7b82e93b497efc5429d2e6860964</book-title>\n" +
                "            \n" +
                "            <lang>en</lang>\n" +
                "            \n" +
                "            \n" +
                "        </title-info>\n" +
                "        <document-info>\n" +
                "            <author><first-name></first-name><last-name>Unknown</last-name></author>\n" +
                "            <program-used>calibre 2.85.1</program-used>\n" +
                "            <date>6.6.2018</date>\n" +
                "            <id>81c4c85a-5fc1-444b-8c47-2382cac24cdd</id>\n" +
                "            <version>1.0</version>\n" +
                "        </document-info>\n" +
                "        <publish-info>\n" +
                "            \n" +
                "            \n" +
                "            \n" +
                "        </publish-info>\n" +
                "    </description>\n"+
                "</FictionBook>\n";

        InputStream is = null;
        pr.parseFb2(is, text);
    }

    @Test
    public void testChangeCSS() {
        Navigator nr = new Navigator();
        nr.changeCSS(1, new String[]{"#000000", "#ffffff", "San Serif", "80", "0.8", "1.25", "5", "5"});

    }

    @Test
    public void testOpenBook() {
        Navigator nr = new Navigator();
        nr.openBook("file://book.fb2", 1);
    }

    @Test
    public void listBook() {
        FileChooser fc = new FileChooser();
        File dir = new File("file://mina");

        fc.bookList(dir);
    }

    @Test
    public void getIdBook() throws Exception {
        Context context = null;
        Manipulator mn = new Manipulator("fileName", "destFolder", context);
        mn.getPageIndex("123547");
    }

    @Test
    public void displayContact() {
        onView(withId(R.id.aboutprog)).perform(click());
    }

    @Test
    public void bookList() {
        onView(withId(R.id.fileListView)).perform(click());
    }

    @Test
    public void settingsTest() {
        onData(hasToString(startsWith("Item Text")))
                .inAdapterView(withId(R.id.spinnerAlign)).atPosition(0)
                .perform(click());
    }

    @Test
    public void settingsT1est() {
        onView(withId(R.id.MainLayout)).perform(swipeLeft());
    }
}