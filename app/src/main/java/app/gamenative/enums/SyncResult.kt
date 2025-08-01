package app.gamenative.enums

enum class SyncResult(val text: String) {
    Success("Success"),
    UpToDate("Up to date"),
    InProgress("In progress"),
    PendingOperations("Pending operations"),
    Conflict("Conflict"),
    UpdateFail("Update failure"),
    DownloadFail("Download failure"),
    CloudAccessIssue("Cloud access issue"),
    UnknownFail("Unknown failure"),
}
