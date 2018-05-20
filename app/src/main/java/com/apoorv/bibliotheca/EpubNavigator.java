package com.apoorv.bibliotheca;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.io.IOException;

public class EpubNavigator {
	private int nBooks;
	private EpubManipulator[] books;
	private SplitPanel[] views;
	private boolean[] extractAudio;
	private boolean synchronizedReadingActive;
	private boolean parallelText = false;
	private MainView activity;
	private Metadata activity_meta;
	private static Context context;

	public EpubNavigator(int numberOfBooks, Metadata activityM) {
		nBooks = numberOfBooks;
		books = new EpubManipulator[this.nBooks];
		views = new SplitPanel[nBooks];
		extractAudio = new boolean[nBooks];
		activity_meta = activityM;
		context = activityM.getBaseContext();
	}

	public EpubNavigator(int numberOfBooks, MainView activityMV) {
		nBooks = numberOfBooks;
		books = new EpubManipulator[nBooks];
		views = new SplitPanel[nBooks];
		extractAudio = new boolean[nBooks];
		activity = activityMV;
		context = activityMV.getBaseContext();
	}

	public boolean openBook(String path, int index) {
		try {
			if (books[index] != null)
				books[index].destroy();

			books[index] = new EpubManipulator(path, index + "", context);
			changePanel(new BookView(), index);
			setBookPage(books[index].getSpineElementPath(0), index);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void setBookPage(String page, int index) {
		if (books[index] != null) {
			books[index].goToPage(page);
		}

		loadPageIntoView(page, index);
	}

	// установите страницу на следующей панели
	public void setNote(String page, int index) {
		loadPageIntoView(page, (index + 1) % nBooks);
	}

	public void loadPageIntoView(String pathOfPage, int index) {
		ViewStateEnum state = ViewStateEnum.notes;

		if (books[index] != null)
			if ((pathOfPage.equals(books[index].getCurrentPageURL()))
					|| (books[index].getPageIndex(pathOfPage) >= 0))
				state = ViewStateEnum.books;

		if (books[index] == null)
			state = ViewStateEnum.notes;

		if (views[index] == null || !(views[index] instanceof BookView))
			changePanel(new BookView(), index);

		((BookView) views[index]).state = state;
		((BookView) views[index]).loadPage(pathOfPage);
	}

	// если синхронизированное чтение активно, измените главу в каждой книге
	public void goToNextChapter(int book) throws Exception {
		setBookPage(books[book].goToNextChapter(), book);

		if (synchronizedReadingActive)
			for (int i = 1; i < nBooks; i++)
				if (books[(book + i) % nBooks] != null)
					setBookPage(books[(book + i) % nBooks].goToNextChapter(),
							(book + i) % nBooks);
	}

	// если синхронизированное чтение активно, измените главу в каждой книге
	public void goToPrevChapter(int book) throws Exception {
		setBookPage(books[book].goToPreviousChapter(), book);

		if (synchronizedReadingActive)
			for (int i = 1; i < nBooks; i++)
				if (books[(book + i) % nBooks] != null)
					setBookPage(
							books[(book + i) % nBooks].goToPreviousChapter(),
							(book + i) % nBooks);
	}

	public void closeView(int index) {
		// case: note or another panel over a book
		if (books[index] != null
				&& (!(views[index] instanceof BookView) || (((BookView) views[index]).state != ViewStateEnum.books))) {
			BookView v = new BookView();
			changePanel(v, index);
			v.loadPage(books[index].getCurrentPageURL());
		} else // all other cases
		{
			if (books[index] != null)
				try {
					books[index].destroy();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			activity.removePanel(views[index]);

			while (index < nBooks - 1) {
				books[index] = books[index + 1]; // shift left all books
				if (books[index] != null) // updating their folder
					books[index].changeDirName(index + ""); // according to the
															// index

				views[index] = views[index + 1]; // shift left every panel
				if (views[index] != null) {
					views[index].setKey(index); // update the panel key
					if (views[index] instanceof BookView
							&& ((BookView) views[index]).state == ViewStateEnum.books)
						((BookView) views[index]).loadPage(books[index]
								.getCurrentPageURL()); // reload the book page
				}
				index++;
			}
			books[nBooks - 1] = null; // last book and last view
			views[nBooks - 1] = null; // don't exist anymore
		}
	}

	public boolean synchronizeView(int from, int to) throws Exception {
		setBookPage(books[to].goToPage(books[from].getCurrentSpineElementIndex()), to);

		return true;
	}

	// экран методаты
	public boolean displayMetadata(int book) {
		boolean res = true;

		if (this.books[book] != null) {
			DataView dv = new DataView();
			dv.loadData(this.books[book].metadata());
			changePanel(dv, book);
		} else
			res = false;

		return res;
	}

    // change the panel in position "index" with the new panel p
    public void changePanel(SplitPanel p, int index) {
        if (views[index] != null) {
            activity.removePanelWithoutClosing(views[index]);
            p.changeWeight(views[index].getWeight());
        }

        if (p.isAdded())
            activity.removePanelWithoutClosing(p);

        views[index] = p;
        activity.addPanel(p);
        p.setKey(index);

        for (int i = index + 1; i < views.length; i++)
            if (views[i] != null) {
                activity.detachPanel(views[i]);
                activity.attachPanel(views[i]);
            }
    }

	// возвращаем TOC файл по запросу
	public boolean displayTOC(int book) {
		boolean res = true;

		if (books[book] != null)
			setBookPage(books[book].tableOfContents(), book);
		else
			res = false;
		return res;
	}

	public void changeCSS(int book, String[] settings) {
		books[book].addCSS(settings);
		loadPageIntoView(books[book].getCurrentPageURL(), book);
	}

	public void changeViewsSize(float weight) {
		if (views[0] != null && views[1] != null) {
			views[0].changeWeight(1 - weight);
			views[1].changeWeight(weight);
		}
	}

	// TODO: update when a new SplitPanel's inherited class is created
	private SplitPanel newPanelByClassName(String className) {
		if (className.equals(BookView.class.getName()))
			return new BookView();
		if (className.equals(DataView.class.getName()))
			return new DataView();

		return null;
	}

	public void saveState(Editor editor) {
		editor.putBoolean(getS(R.string.sync), synchronizedReadingActive);
		editor.putBoolean(getS(R.string.parallelTextBool), parallelText);

		// Save Books
		for (int i = 0; i < nBooks; i++)
			if (books[i] != null) {
				editor.putInt(getS(R.string.CurrentPageBook) + i,
						books[i].getCurrentSpineElementIndex());
				editor.putInt(getS(R.string.LanguageBook) + i,
						books[i].getCurrentLanguage());
				editor.putString(getS(R.string.nameEpub) + i,
						books[i].getDecompressedFolder());
				editor.putString(getS(R.string.pathBook) + i,
						books[i].getFileName());
				try {
					books[i].closeStream();
				} catch (IOException e) {
					Log.e(getS(R.string.error_CannotCloseStream),
							getS(R.string.Book_Stream) + (i + 1));
					e.printStackTrace();
				}
			} else {
				editor.putInt(getS(R.string.CurrentPageBook) + i, 0);
				editor.putInt(getS(R.string.LanguageBook) + i, 0);
				editor.putString(getS(R.string.nameEpub) + i, null);
				editor.putString(getS(R.string.pathBook) + i, null);
			}

		// Save views
		for (int i = 0; i < nBooks; i++)
			if (views[i] != null) {
				editor.putString(getS(R.string.ViewType) + i, views[i]
						.getClass().getName());
				views[i].saveState(editor);
				activity.removePanelWithoutClosing(views[i]);
			} else
				editor.putString(getS(R.string.ViewType) + i, "");
	}

	public boolean loadState(SharedPreferences preferences) {
		boolean ok = true;
		synchronizedReadingActive = preferences.getBoolean(getS(R.string.sync),
				false);
		parallelText = preferences.getBoolean(getS(R.string.parallelTextBool),
				false);

		int current, lang;
		String name, path;
		for (int i = 0; i < nBooks; i++) {
			current = preferences.getInt(getS(R.string.CurrentPageBook) + i, 0);
			lang = preferences.getInt(getS(R.string.LanguageBook) + i, 0);
			name = preferences.getString(getS(R.string.nameEpub) + i, null);
			path = preferences.getString(getS(R.string.pathBook) + i, null);
			extractAudio[i] = preferences.getBoolean(
					getS(R.string.exAudio) + i, false);
			// try loading a book already extracted
			if (path != null) {
				try {
					books[i] = new EpubManipulator(path, name, current, lang,
							context);
					books[i].goToPage(current);
				} catch (Exception e1) {
					try {
						books[i] = new EpubManipulator(path, i + "", context);
						books[i].goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				} catch (Error e) {
					try {
						books[i] = new EpubManipulator(path, i + "", context);
						books[i].goToPage(current);
					} catch (Exception e2) {
						ok = false;
					} catch (Error e3) {
						ok = false;
					}
				}
			} else
				books[i] = null;
		}

		return ok;
	}

	public void loadViews(SharedPreferences preferences) {
		for (int i = 0; i < nBooks; i++) {
			views[i] = newPanelByClassName(preferences.getString(
					getS(R.string.ViewType) + i, ""));
			if (views[i] != null) {
				activity.addPanel(views[i]);
				views[i].setKey(i);
				views[i].loadState(preferences);
			}
		}
	}

	public String getS(int id) {
		return context.getResources().getString(id);
	}
}