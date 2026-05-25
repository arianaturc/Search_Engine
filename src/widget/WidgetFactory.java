package widget;

import search.SearchResult;

import java.util.ArrayList;
import java.util.List;


public class WidgetFactory {

    private final List<Widget> availableWidgets;

    public WidgetFactory() {
        this.availableWidgets = new ArrayList<>();

        availableWidgets.add(new GalleryWidget());
        availableWidgets.add(new LogAnalyzerWidget());
        availableWidgets.add(new CodeAnalyzerWidget());
    }

    public List<Widget> getActiveWidgets(List<SearchResult> results, String query) {
        List<Widget> active = new ArrayList<>();
        for (Widget widget : availableWidgets) {
            if (widget.shouldActivate(results, query)) {
                active.add(widget);
            }
        }
        return active;
    }

    public void registerWidget(Widget widget) {
        availableWidgets.add(widget);
    }

    public List<Widget> getAllWidgets() {
        return List.copyOf(availableWidgets);
    }
}