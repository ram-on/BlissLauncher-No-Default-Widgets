package foundation.e.blisslauncher.features.launcher;


import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.BlissFrameLayout;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.features.suggestions.AutoCompleteAdapter;
import foundation.e.blisslauncher.features.suggestions.SuggestionsResult;
import foundation.e.blisslauncher.features.test.TestActivity;
import io.reactivex.observers.DisposableObserver;
import java.util.ArrayList;

public class SearchInputDisposableObserver extends DisposableObserver<SuggestionsResult> {

    private AutoCompleteAdapter networkSuggestionAdapter;
    private TestActivity launcherActivity;
    private ViewGroup appSuggestionsViewGroup;

    public SearchInputDisposableObserver(TestActivity activity, RecyclerView.Adapter autoCompleteAdapter, ViewGroup viewGroup) {
        this.networkSuggestionAdapter = (AutoCompleteAdapter) autoCompleteAdapter;
        this.launcherActivity = activity;
        this.appSuggestionsViewGroup = viewGroup;
    }

    @Override
    public void onNext(SuggestionsResult suggestionsResults) {
        if (suggestionsResults.type
                == SuggestionsResult.TYPE_NETWORK_ITEM) {
            networkSuggestionAdapter.updateSuggestions(suggestionsResults.getNetworkItems(),
                    suggestionsResults.queryText);
        } else if (suggestionsResults.type
                == SuggestionsResult.TYPE_LAUNCHER_ITEM) {
            ((ViewGroup)appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid)).removeAllViews();
            appSuggestionsViewGroup.findViewById(R.id.openUsageAccessSettings).setVisibility(View.GONE);
            appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid).setVisibility(View.VISIBLE);
            for (LauncherItem launcherItem : suggestionsResults
                    .getLauncherItems()) {
                BlissFrameLayout blissFrameLayout = launcherActivity.prepareSuggestedApp(
                        launcherItem);
                launcherActivity.addAppToGrid(appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid), blissFrameLayout);
            }
        } else {
            launcherActivity.getLauncherPagedView().refreshSuggestedApps(appSuggestionsViewGroup,true);
            networkSuggestionAdapter.updateSuggestions(new ArrayList<>(),
                    suggestionsResults.queryText);
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
}
