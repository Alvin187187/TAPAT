package com.tapat.ui;

import com.tapat.AppContext;
import com.tapat.model.AppModule;
import com.tapat.model.Application;
import com.tapat.model.User;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.util.function.Consumer;

public class Navigator {
    private final Session session;
    private final BorderPane content;
    private final Consumer<AppModule> highlight;
    private final DirectoryView directory;
    private final FundSummaryView fundSummary;
    private final MyApplicationsView applications;
    private final ApplicationFormView applicationForm;
    private final ProgramEditorView programs;
    private final ReviewQueueView review;
    private final FinanceView finance;
    private final ReportView reports;
    private final NoticeView notices;
    private final StaffView staff;
    private final SignInView signIn;
    private final RegisterView register;

    public Navigator(AppContext context, Session session, BorderPane content, Consumer<AppModule> highlight) {
        this.session = session;
        this.content = content;
        this.highlight = highlight;
        this.directory = new DirectoryView(context, session, this);
        this.fundSummary = new FundSummaryView(context, session, this);
        this.applications = new MyApplicationsView(context, session, this);
        this.applicationForm = new ApplicationFormView(context, session, this);
        this.programs = new ProgramEditorView(context, session, this);
        this.review = new ReviewQueueView(context, session, this);
        this.finance = new FinanceView(context, session, this);
        this.reports = new ReportView(context, session, this);
        this.notices = new NoticeView(context, session, this);
        this.staff = new StaffView(context, session, this);
        this.signIn = new SignInView(context, session, this);
        this.register = new RegisterView(context, session, this);
    }

    public void show(AppModule module) {
        if (module == AppModule.DIRECTORY) {
            show(directory.getRoot(), directory::refresh, module);
        } else if (module == AppModule.FUND_SUMMARY) {
            show(fundSummary.getRoot(), fundSummary::refresh, module);
        } else if (module == AppModule.MY_APPLICATIONS) {
            show(applications.getRoot(), applications::refresh, module);
        } else if (module == AppModule.PROGRAMS) {
            show(programs.getRoot(), programs::refresh, module);
        } else if (module == AppModule.REVIEW) {
            show(review.getRoot(), review::refresh, module);
        } else if (module == AppModule.FUNDS) {
            show(finance.getRoot(), finance::refresh, module);
        } else if (module == AppModule.REPORTS) {
            show(reports.getRoot(), reports::refresh, module);
        } else if (module == AppModule.NOTICES) {
            show(notices.getRoot(), notices::refresh, module);
        } else if (module == AppModule.STAFF) {
            show(staff.getRoot(), staff::refresh, module);
        }
    }

    public void showSignIn() {
        show(signIn.getRoot(), signIn::refresh, null);
    }

    public void showRegister() {
        show(register.getRoot(), register::refresh, null);
    }

    public void showHome() {
        show(homeModule(session.current().orElse(null)));
    }

    public void openApplication(int programId) {
        show(applicationForm.getRoot(), () -> applicationForm.open(programId), AppModule.DIRECTORY);
    }

    public void openCase(Application application, String token) {
        if (application == null) {
            showHome();
            return;
        }
        User actor = session.current().orElse(null);
        if (actor != null && actor.canRecordFunds() && token != null && token.startsWith("TPT-R-")) {
            finance.selectClaim(token);
            finance.selectPayable(application.getId());
            show(AppModule.FUNDS);
            return;
        }
        AppModule home = homeModule(actor);
        if (home == AppModule.MY_APPLICATIONS) {
            applications.selectReference(application.getReferenceNo());
            show(AppModule.MY_APPLICATIONS);
        } else if (home == AppModule.REVIEW) {
            review.selectReference(application.getReferenceNo());
            show(AppModule.REVIEW);
        } else if (home == AppModule.FUNDS) {
            finance.selectClaim(application.getClaimReference() == null ? token : application.getClaimReference());
            finance.selectPayable(application.getId());
            show(AppModule.FUNDS);
        } else if (home == AppModule.REPORTS) {
            reports.selectReference(application.getReferenceNo());
            show(AppModule.REPORTS);
        } else {
            show(AppModule.DIRECTORY);
        }
    }

    public static AppModule homeModule(User actor) {
        if (actor == null) {
            return AppModule.DIRECTORY;
        }
        if (actor.canReviewApplications()) {
            return AppModule.REVIEW;
        }
        if (actor.canRecordFunds()) {
            return AppModule.FUNDS;
        }
        if (actor.canExportFullReports()) {
            return AppModule.REPORTS;
        }
        if (actor.canSubmitApplications()) {
            return AppModule.MY_APPLICATIONS;
        }
        return AppModule.DIRECTORY;
    }

    private void show(Parent root, Runnable refresh, AppModule module) {
        content.setCenter(root);
        if (root instanceof javafx.scene.layout.Region) {
            javafx.scene.layout.Region region = (javafx.scene.layout.Region) root;
            region.prefWidthProperty().unbind();
            region.prefHeightProperty().unbind();
            region.minWidthProperty().unbind();
            region.minHeightProperty().unbind();
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        refresh.run();
        highlight.accept(module);
    }
}
