package com.tapat;

import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.DocumentRepository;
import com.tapat.repository.FundRepository;
import com.tapat.repository.NotificationRepository;
import com.tapat.repository.ProgramRepository;
import com.tapat.repository.UserRepository;
import com.tapat.repository.sqlite.Database;
import com.tapat.repository.sqlite.SchemaInitializer;
import com.tapat.repository.sqlite.SeedData;
import com.tapat.repository.sqlite.SqliteApplicationRepository;
import com.tapat.repository.sqlite.SqliteAuditRepository;
import com.tapat.repository.sqlite.SqliteDocumentRepository;
import com.tapat.repository.sqlite.SqliteFundRepository;
import com.tapat.repository.sqlite.SqliteNotificationRepository;
import com.tapat.repository.sqlite.SqliteProgramRepository;
import com.tapat.repository.sqlite.SqliteUserRepository;
import com.tapat.security.PasswordHasher;
import com.tapat.service.ApplicationService;
import com.tapat.service.AuthService;
import com.tapat.service.CaseReview;
import com.tapat.service.DocumentStore;
import com.tapat.service.FundService;
import com.tapat.service.NotificationService;
import com.tapat.service.ProgramService;
import com.tapat.service.ReportService;
import com.tapat.service.ReviewService;
import com.tapat.service.eligibility.EligibilityChecker;

import java.nio.file.Path;

public class AppContext {
    private final Database database;
    private final AuthService auth;
    private final ProgramService programs;
    private final ApplicationService applications;
    private final ReviewService review;
    private final FundService funds;
    private final NotificationService notifications;
    private final ReportService reports;
    private final DocumentStore documents;
    private final CaseReview cases;
    private Runnable onDataChanged;

    private AppContext(Database database, AuthService auth, ProgramService programs, ApplicationService applications,
                       ReviewService review, FundService funds, NotificationService notifications, ReportService reports,
                       DocumentStore documents, CaseReview cases) {
        this.database = database;
        this.auth = auth;
        this.programs = programs;
        this.applications = applications;
        this.review = review;
        this.funds = funds;
        this.notifications = notifications;
        this.reports = reports;
        this.documents = documents;
        this.cases = cases;
    }

    public static AppContext open(Path dataDir) {
        Database database = Database.open(dataDir.resolve("tapat.db"));
        SchemaInitializer.apply(database);
        UserRepository users = new SqliteUserRepository(database);
        ProgramRepository programs = new SqliteProgramRepository(database);
        ApplicationRepository applications = new SqliteApplicationRepository(database);
        DocumentRepository documents = new SqliteDocumentRepository(database);
        FundRepository funds = new SqliteFundRepository(database);
        NotificationRepository notifications = new SqliteNotificationRepository(database);
        AuditRepository audit = new SqliteAuditRepository(database);
        PasswordHasher hasher = new PasswordHasher();
        DocumentStore store = new DocumentStore(dataDir);
        SeedData.seedIfEmpty(users, programs, applications, documents, funds, notifications, audit, store, hasher);

        NotificationService notificationService = new NotificationService(notifications, applications, programs, users);
        AuthService auth = new AuthService(users, audit, notificationService, hasher);
        ProgramService programService = new ProgramService(programs, audit);
        ApplicationService applicationService = new ApplicationService(
                applications, documents, programService, audit, notificationService, EligibilityChecker.standard(), store);
        ReviewService review = new ReviewService(applications, applicationService, programService, notificationService, audit, funds);
        FundService fundService = new FundService(database, funds, programs, applications, notificationService, audit);
        ReportService reports = new ReportService(fundService, applications, funds);
        CaseReview cases = new CaseReview(applications, documents, users, programService, audit, EligibilityChecker.standard());
        SeedData.ensureClaimDemo(users, programs, applications, funds, audit, hasher, documents, store);
        return new AppContext(database, auth, programService, applicationService, review, fundService,
                notificationService, reports, store, cases);
    }

    public AuthService auth() {
        return auth;
    }

    public ProgramService programs() {
        return programs;
    }

    public ApplicationService applications() {
        return applications;
    }

    public ReviewService review() {
        return review;
    }

    public FundService funds() {
        return funds;
    }

    public NotificationService notifications() {
        return notifications;
    }

    public ReportService reports() {
        return reports;
    }

    public DocumentStore documents() {
        return documents;
    }

    public CaseReview cases() {
        return cases;
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public void changed() {
        if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    public void close() {
        database.close();
    }
}
