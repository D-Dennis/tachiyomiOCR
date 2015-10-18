package eu.kanade.mangafeed.presenter;

import android.os.Bundle;

import com.pushtorefresh.storio.sqlite.operations.post.PostResult;

import java.util.List;

import javax.inject.Inject;

import eu.kanade.mangafeed.data.helpers.DatabaseHelper;
import eu.kanade.mangafeed.data.helpers.SourceManager;
import eu.kanade.mangafeed.data.models.Chapter;
import eu.kanade.mangafeed.data.models.Manga;
import eu.kanade.mangafeed.ui.fragment.MangaChaptersFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MangaChaptersPresenter extends BasePresenter<MangaChaptersFragment> {

    @Inject DatabaseHelper db;
    @Inject SourceManager sourceManager;

    private Manga manga;

    private static final int DB_CHAPTERS = 1;
    private static final int ONLINE_CHAPTERS = 2;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(DB_CHAPTERS,
                this::getDbChaptersObs,
                MangaChaptersFragment::onNextChapters
        );

        restartableLatestCache(ONLINE_CHAPTERS,
                this::getOnlineChaptersObs,
                (view, result) -> view.onNextOnlineChapters()
        );
    }

    @Override
    protected void onTakeView(MangaChaptersFragment view) {
        super.onTakeView(view);
        registerForStickyEvents();
    }

    @Override
    protected void onDropView() {
        unregisterForEvents();
        super.onDropView();
    }

    public void onEventMainThread(Manga manga) {
        if (this.manga == null) {
            this.manga = manga;
            start(DB_CHAPTERS);
        }
    }

    public void refreshChapters(MangaChaptersFragment view) {
        if (manga != null) {
            view.setSwipeRefreshing();
            start(ONLINE_CHAPTERS);
        }
    }

    private Observable<List<Chapter>> getDbChaptersObs() {
        return db.getChapters(manga.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PostResult> getOnlineChaptersObs() {
        return sourceManager.get(manga.source)
                .pullChaptersFromNetwork(manga.url)
                .subscribeOn(Schedulers.io())
                .flatMap(chapters -> db.insertOrRemoveChapters(manga, chapters))
                .observeOn(AndroidSchedulers.mainThread());
    }
}
