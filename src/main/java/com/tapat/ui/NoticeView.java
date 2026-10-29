package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.Application;
import com.tapat.model.Notification;
import com.tapat.model.User;
import com.tapat.util.Text;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoticeView {
    private static final Pattern REFERENCE = Pattern.compile("(TAPAT-\\d{4}-\\d+|TPT-R-\\d+)", Pattern.CASE_INSENSITIVE);

    private final AppContext context;
    private final Session session;
    private final Navigator navigator;
    private final ComboBox<String> filter = UiKit.combo(List.of("Unread", "All"));
    private final VBox list = new VBox(8);
    private final VBox detail = new VBox(8);
    private final Label error = UiKit.error();
    private final Label summary = UiKit.meta("");
    private final BorderPane root;
    private List<Notification> all = List.of();
    private Notification selected;

    public NoticeView(AppContext context, Session session, Navigator navigator) {
        this.context = context;
        this.session = session;
        this.navigator = navigator;
        filter.setValue("Unread");
        filter.setPrefWidth(140);
        filter.setMaxWidth(140);
        filter.setOnAction(event -> renderList());
        Button markAll = UiKit.secondary("Mark all read");
        markAll.setOnAction(event -> markAllRead());
        detail.getStyleClass().add("panel");
        UiKit.watch(detail);
        detail.setMinWidth(0);
        VBox inbox = new VBox(8, UiKit.toolbar(filter, markAll), summary, list, error);
        inbox.setMinWidth(0);
        UiKit.grow(inbox);
        this.root = UiKit.workspace(
                UiKit.split(inbox, UiKit.sideScroll(detail)),
                UiKit.title("Inbox"),
                UiKit.body("These are status messages. Open one to jump to that case."));
        showDetail(null);
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        UiKit.clear(error);
        selected = null;
        if (session.current().isEmpty()) {
            all = List.of();
            list.getChildren().setAll(UiKit.body("Sign in to see notices for your account."));
            summary.setText("");
            showDetail(null);
            return;
        }
        try {
            all = context.notifications().list(session.current().get());
            long unread = all.stream().filter(item -> !item.isRead()).count();
            summary.setText(unread == 0
                    ? all.size() + (all.size() == 1 ? " notice." : " notices.")
                    : unread + " unread. Open one to go to the case.");
            if (unread == 0) {
                filter.setValue("All");
            }
            renderList();
        } catch (RuntimeException ex) {
            UiKit.showError(error, ex);
        }
    }

    private void renderList() {
        list.getChildren().clear();
        boolean unreadOnly = "Unread".equals(filter.getValue());
        List<Notification> shown = new ArrayList<>();
        for (Notification item : all) {
            if (!unreadOnly || !item.isRead()) {
                shown.add(item);
            }
        }
        if (shown.isEmpty()) {
            list.getChildren().add(UiKit.body(unreadOnly
                    ? "Nothing unread. Switch the filter to All to see earlier notices."
                    : "No notices yet. Approvals, releases, and deadlines appear here."));
            showDetail(null);
            return;
        }
        if (selected == null || shown.stream().noneMatch(item -> item.getId() == selected.getId())) {
            selected = shown.get(0);
        }
        for (Notification item : shown) {
            list.getChildren().add(row(item));
        }
        showDetail(selected);
    }

    private VBox row(Notification item) {
        Label title = new Label(item.getTitle());
        title.getStyleClass().add(item.isRead() ? "notice-title" : "notice-title-unread");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        Label preview = UiKit.meta(oneLine(item.getMessage()) + "  ·  " + Text.dateTime(item.getCreatedAt()));
        VBox card = new VBox(3, title, preview);
        card.getStyleClass().add("notice-row");
        if (!item.isRead()) {
            card.getStyleClass().add("notice-row-unread");
        }
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> {
            selected = item;
            if (!item.isRead() && session.current().isPresent()) {
                context.notifications().markRead(session.current().get(), item.getId());
                context.changed();
                all = context.notifications().list(session.current().get());
            }
            renderList();
        });
        return card;
    }

    private void showDetail(Notification item) {
        detail.getChildren().clear();
        if (item == null) {
            detail.getChildren().add(UiKit.section("Notice"));
            detail.getChildren().add(UiKit.body("Select a notice on the left. The Open button takes you to the related application."));
            return;
        }
        detail.getChildren().add(UiKit.section(item.getTitle()));
        detail.getChildren().add(UiKit.meta(Text.dateTime(item.getCreatedAt()) + (item.isRead() ? "  ·  Read" : "  ·  Unread")));
        detail.getChildren().add(UiKit.body(item.getMessage()));
        String token = token(item);
        String action = actionLabel(item, token);
        if (action != null) {
            Button open = UiKit.primary(action);
            open.setMaxWidth(Double.MAX_VALUE);
            open.setOnAction(event -> openRelated(item, token));
            detail.getChildren().add(open);
        }
        if (token != null) {
            detail.getChildren().add(UiKit.fact("Linked record", token));
        }
    }

    private void openRelated(Notification item, String token) {
        if (session.current().isEmpty()) {
            return;
        }
        User actor = session.current().get();
        if (!item.isRead()) {
            context.notifications().markRead(actor, item.getId());
            context.changed();
        }
        if (item.getTitle().toLowerCase().contains("deadline")) {
            navigator.show(com.tapat.model.AppModule.DIRECTORY);
            return;
        }
        if (token == null) {
            home(actor);
            return;
        }
        Application match = context.applications().findByNotice(actor, token).orElse(null);
        if (match == null) {
            home(actor);
            return;
        }
        navigator.openCase(match, token);
    }

    private void home(User actor) {
        if (actor.canSubmitApplications()) {
            navigator.show(com.tapat.model.AppModule.MY_APPLICATIONS);
        } else if (actor.canReviewApplications()) {
            navigator.show(com.tapat.model.AppModule.REVIEW);
        } else if (actor.canRecordFunds()) {
            navigator.show(com.tapat.model.AppModule.FUNDS);
        } else {
            navigator.show(com.tapat.model.AppModule.REPORTS);
        }
    }

    private String actionLabel(Notification item, String token) {
        if (item.getTitle().toLowerCase().contains("deadline")) {
            return "Open programs";
        }
        if (token == null) {
            return "Go to my work";
        }
        if (token.startsWith("TPT-R-")) {
            return "Open claim " + token;
        }
        return "Open " + token;
    }

    private void markAllRead() {
        if (session.current().isEmpty()) {
            return;
        }
        context.notifications().markAllRead(session.current().get());
        context.changed();
        refresh();
    }

    private static String token(Notification item) {
        Matcher matcher = REFERENCE.matcher(item.getTitle() + " " + item.getMessage());
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private static String oneLine(String message) {
        if (message == null) {
            return "";
        }
        String compact = message.replace('\n', ' ').trim();
        return compact.length() > 90 ? compact.substring(0, 87) + "…" : compact;
    }
}
