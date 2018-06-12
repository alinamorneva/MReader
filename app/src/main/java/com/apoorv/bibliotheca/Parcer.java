package com.apoorv.bibliotheca;

import android.app.Fragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.util.Pair;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import nl.siegmann.epublib.domain.Book;

public class Parcer extends Fragment {
    private Book book;
    private final int TEXT_CHUNK_SIZE = 15000;
    private final int CHAPTER_SIZE = 17000;
    private final int CHAPTER_SIZE_SMALL = 2000;
    private final int INDENT = 4;
    private ArrayList<Pair<String, Integer>> lines_for_web;
    private String file_name;
    private String FB2 = "FileFragment";
    private final Map<String, String> subTagsMapStart = new HashMap();
    private final Map<String, String> subTagsMapEnd = new HashMap();
    private final String[] headerNames = {"isbn", "book-name", "book-title"};
    private final String[] bookBodyTags = {"annotation", "epigraph", "body"};
    private final String[] notesAttrs = {"notes"};
    private final String[] notesTags = {"body"};
    private final String[] notesSubAttrTags = {"section"};
    private final String[] notesSubAttrNames = {"id"};
    private final String[] notesTitleTags = {"title"};
    private final String[] binTags = {"binary"};
    private final String[] binContAttrs = {"content-type"};
    private final String[] binIdAttrs = {"id"};

    private void initializeMap() {
        subTagsMapStart.put("title", "<div style=\"text-align:center;\"><h3>");
        subTagsMapStart.put("emphasis", "<em>");
        subTagsMapStart.put("strong", "<strong>");
        subTagsMapStart.put("strikethrough", "<strike>");
        subTagsMapStart.put("sub", "<sub>");
        subTagsMapStart.put("sup", "<sup>");
        subTagsMapStart.put("empty-line", "<br>");
        subTagsMapStart.put("v", "<br>");
        subTagsMapStart.put("a", "<a>");
        subTagsMapStart.put("p", "<br>");

        subTagsMapEnd.put("title", "</h3></div>");
        subTagsMapEnd.put("emphasis", "</em>");
        subTagsMapEnd.put("strong", "</strong>");
        subTagsMapEnd.put("strikethrough", "</strike>");
        subTagsMapEnd.put("sub", "</sub>");
        subTagsMapEnd.put("sup", "</sup>");
        subTagsMapEnd.put("empty-line", "");
        subTagsMapEnd.put("v", "<br>");
        subTagsMapEnd.put("a", "</a>");
        subTagsMapEnd.put("p", "");
    }

    public static Parcer getInstance(String filename) {
        Parcer pr = new Parcer();
        pr.file_name = filename;
        return pr;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    synchronized public WeakReference<ArrayList<Pair<String, Integer>>> getPreparedBook() {
        if (lines_for_web != null) {
            return new WeakReference<ArrayList<Pair<String, Integer>>>(lines_for_web);
        } else {
            return null;
        }
    }

    public void parseFb2(InputStream inputStream, String text) {
        Spanned body;
        Map<String, String> headers = new HashMap<String, String>();
        String noteId = "";
        String binContent = "";
        String binContentType = "";
        String binContentId = "";
        XmlPullParserFactory parserFactory = null;
        boolean bookIsOpen = false;
        boolean headerFound = false;
        boolean notesFound = false;
        boolean noteSectionFound = false;
        boolean binFound = false;
        int headerNumber = -1;
        StringBuilder builder = new StringBuilder();
        StringBuilder builder_notes = new StringBuilder();
        Stack<String> tagStack = new Stack<String>();
        Stack<String> bodyTagStack = new Stack<String>();
        ArrayList<String> bookNotes = null;
        AbstractList arrayList = null;

        try {
            parserFactory = XmlPullParserFactory.newInstance();
            parserFactory.setNamespaceAware(false);
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setInput(inputStream, null);
            int eventType = parser.getEventType();
            headers.put("file-name", file_name);
            while (true) {
                //check if end of document is reached
                if (eventType == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                //check headers
                for (int i = 0; i < headerNames.length; i++) {
                    if (eventType == XmlPullParser.START_TAG && parser.getName().equals(headerNames[i])) {
                        eventType = parser.next();
                        headerFound = true;
                        headerNumber = i;
                        break;
                    }
                }

                for (int i = 0; i < headerNames.length; i++) {
                    if (eventType == XmlPullParser.END_TAG && parser.getName().equals(headerNames[i])) {
                        eventType = parser.next();
                        headerFound = false;
                        headerNumber = -1;
                        break;
                    }
                }
                for (int i = 0; i < bookBodyTags.length; i++) {
                    if (eventType == XmlPullParser.START_TAG && parser.getAttributeCount() == 0 && parser.getName().equals(bookBodyTags[i])) {
                        bookIsOpen = true;
                        bodyTagStack.push(bookBodyTags[i]);
                        eventType = parser.next();
                        break;
                    }
                }

                for (int i = 0; i < bookBodyTags.length; i++) {
                    if (!notesFound && eventType == XmlPullParser.END_TAG && parser.getName().equals(bookBodyTags[i])) {
                        if (!bodyTagStack.empty() && bodyTagStack.peek().equals(parser.getName()))
                            bodyTagStack.pop();
                        if (bodyTagStack.empty())
                            bookIsOpen = false;
                        eventType = parser.next();
                        break;
                    }
                }
                for (int i = 0; i < notesTags.length; i++) {
                    boolean stop = false;

                    if (eventType == XmlPullParser.START_TAG && parser.getAttributeCount() > 0 && parser.getName().equals(notesTags[i])) {
                        int attrCount = parser.getAttributeCount();
                        for (int j = 0; j < attrCount; j++) {
                            if (stop) {
                                break;
                            }
                            for (int k = 0; k < notesAttrs.length; k++) {
                                if (parser.getAttributeValue(j).equals(notesAttrs[k])) {
                                    notesFound = true;
                                    eventType = parser.next();
                                    stop = true;
                                    break;
                                }
                            }
                        }
                        if (stop) {
                            break;
                        }
                    }
                }

                for (int i = 0; i < notesTags.length; i++) {
                    if (notesFound && eventType == XmlPullParser.END_TAG && parser.getName().equals(notesTags[i])) {
                        notesFound = false;
                        eventType = parser.next();
                        break;
                    }
                }

                for (int i = 0; i < notesSubAttrTags.length; i++) {
                    boolean stop = false;
                    if (notesFound && eventType == XmlPullParser.START_TAG && parser.getName().equals(notesSubAttrTags[i])) {
                        noteSectionFound = true;
                        int attrCount = parser.getAttributeCount();
                        for (int j = 0; j < attrCount; j++) {
                            if (stop) {
                                break;
                            }
                            for (int k = 0; k < notesSubAttrNames.length; k++) {
                                if (parser.getAttributeName(j).equals(notesSubAttrNames[k])) {
                                    noteId = parser.getAttributeValue(j);
                                    eventType = parser.next();
                                    stop = true;
                                    break;
                                }
                            }
                        }
                        if (stop) {
                            break;
                        }
                    }
                }

                for (int i = 0; i < notesSubAttrTags.length; i++) {
                    if (noteSectionFound && eventType == XmlPullParser.END_TAG && parser.getName().equals(notesSubAttrTags[i])) {
                        noteSectionFound = false;
                        bookNotes.add(builder_notes.toString());
                        noteId = "";
                        builder_notes = new StringBuilder();
                        break;
                    }
                }

                for (int i = 0; i < binTags.length; i++) {
                    if (eventType == XmlPullParser.START_TAG && parser.getName().equals(binTags[i])) {
                        boolean contTypeFound = false;
                        boolean contIdFound = false;
                        binFound = true;
                        int attrCount = parser.getAttributeCount();
                        if (attrCount > 0) {
                            for (int j = 0; j < attrCount; j++) {
                                for (int k = 0; k < binContAttrs.length; k++) {
                                    if (parser.getAttributeName(j).equals(binContAttrs[k]) && !contTypeFound) {
                                        binContentType = parser.getAttributeValue(j);
                                        contTypeFound = true;
                                        break;
                                    }
                                }
                                for (int k = 0; k < binIdAttrs.length; k++) {
                                    if (parser.getAttributeName(j).equals(binIdAttrs[k]) && !contIdFound) {
                                        binContentId = parser.getAttributeValue(j);
                                        contIdFound = true;
                                        break;
                                    }
                                }
                            }
                        }
                        eventType = parser.next();
                        break;
                    }
                }

                if (eventType == XmlPullParser.TEXT && headerFound && headerNumber > -1) {
                    headers.put(headerNames[headerNumber], parser.getText());
                }

                if (eventType == XmlPullParser.TEXT && binFound) {
                    binContent = parser.getText();
                }

                if (noteSectionFound && eventType == XmlPullParser.TEXT) {
                    builder_notes.append(parser.getText() + " ");
                }

                if (eventType == XmlPullParser.TEXT && bookIsOpen) {
                    String currentPiece = parser.getText();
                    if (currentPiece.length() > TEXT_CHUNK_SIZE) {
                        if (tagStack.empty() || !tagStack.peek().equals("a")) {
                            if (builder.length() < CHAPTER_SIZE_SMALL) {
                                builder.append(currentPiece);
                                if (!tagStack.empty()) {
                                    for (int i = 0; i < tagStack.size(); i++) {
                                        builder.append(subTagsMapEnd.get(tagStack.pop()));
                                    }
                                }
                                arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                builder = new StringBuilder();
                            } else {
                                if (!tagStack.empty()) {
                                    Stack<String> tagStackCopy = new Stack<String>();
                                    for (int i = 0; i < tagStack.size(); i++) {
                                        String x = tagStack.pop();
                                        tagStackCopy.push(x);
                                        builder.append(subTagsMapEnd.get(x));
                                    }
                                    arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                    builder = new StringBuilder();
                                    for (int i = 0; i < tagStackCopy.size(); i++) {
                                        String y = tagStackCopy.pop();
                                        tagStack.push(y);
                                        builder.append(subTagsMapStart.get(y));
                                    }
                                    builder.append(currentPiece);
                                    for (int i = 0; i < tagStack.size(); i++) {
                                        builder.append(subTagsMapEnd.get(tagStack.pop()));
                                    }
                                    arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                    builder = new StringBuilder();
                                } else {
                                    arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                    builder = new StringBuilder();
                                    builder.append(currentPiece);
                                    arrayList.add((new SpannableStringBuilder(Html.fromHtml(builder.toString()))));
                                    builder = new StringBuilder();
                                }
                            }
                        }
                    } else if (builder.length() > CHAPTER_SIZE) {
                        if (tagStack.empty() || !tagStack.peek().equals("a")) {
                            if (!tagStack.empty()) {
                                Stack<String> tagStackCopy = new Stack<String>();
                                for (int i = 0; i < tagStack.size(); i++) {
                                    String x = tagStack.pop();
                                    tagStackCopy.push(x);
                                    builder.append(subTagsMapEnd.get(x));
                                }
                                arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                builder = new StringBuilder();
                                for (int i = 0; i < tagStackCopy.size(); i++) {
                                    String y = tagStackCopy.pop();
                                    tagStack.push(y);
                                    builder.append(subTagsMapStart.get(y));
                                }
                                builder.append(currentPiece);
                            } else {
                                arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
                                builder = new StringBuilder();
                                builder.append(currentPiece);
                            }
                        }
                    } else {
                        builder.append(currentPiece);
                    }
                } else if (bookIsOpen && eventType == XmlPullParser.START_TAG) {
                    String c_sub_tag = parser.getName();
                    if (c_sub_tag.equals("a")) {
                        String note_id = parser.getAttributeValue(null, "l:href");
                        builder.append("<a href='" + note_id + "'>");
                        tagStack.push("a");
                    }
                    if (c_sub_tag.equals("image")) {
                        builder.append("<img src='" + parser.getAttributeValue(null, "l:href") + "'/>");
                    }
                    for (Map.Entry<String, String> entry : subTagsMapStart.entrySet()
                            ) {
                        if (entry.getKey().equals(c_sub_tag) && !c_sub_tag.equals("a")) {
                            builder.append(entry.getValue());
                            if (c_sub_tag.equals("p")) {
                                if (tagStack.empty() || !tagStack.peek().equals("title")) {
                                    for (int i = 0; i < INDENT; i++) {
                                        builder.append('\u0009');
                                    }
                                }
                            }
                            tagStack.push(entry.getKey());
                        }
                    }
                } else if (bookIsOpen && eventType == XmlPullParser.END_TAG) {
                    String c_sub_tag = parser.getName();
                    for (Map.Entry<String, String> entry : subTagsMapEnd.entrySet()
                            ) {
                        if (entry.getKey().equals(c_sub_tag)) {
                            builder.append(entry.getValue());
                            if (!tagStack.empty() && tagStack.peek().equals(entry.getKey())) {
                                tagStack.pop();
                            }
                        }
                    }
                } else if (!bookIsOpen && eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("image")) {
                        if (arrayList.isEmpty()) {
                            builder.insert(0, "<img src='" + parser.getAttributeValue(null, "l:href") + "'/>");
                        } else {
                            builder.append("<img src='" + parser.getAttributeValue(null, "l:href") + "'/>");
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            Log.d(FB2, "XmlPullParserException", e);
            body = Html.fromHtml("Error reading file");
        } catch (IOException e) {
            Log.d(FB2, "I/O XmlPullParserException", e);
            body = Html.fromHtml("Error reading file");
        }
        arrayList.add(new SpannableStringBuilder(Html.fromHtml(builder.toString())));
    }
}