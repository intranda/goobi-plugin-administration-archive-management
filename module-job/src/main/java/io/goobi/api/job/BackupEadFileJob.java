package io.goobi.api.job;

import org.goobi.production.flow.jobs.AbstractGoobiJob;

public class BackupEadFileJob extends AbstractGoobiJob {

    @Override
    public String getJobName() {
        return "intranda_quartz_backupEadFile";
    }

    @Override
    public void execute() {

        String backupFolder;

    }
}
