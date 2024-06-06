package io.goobi.api.job;

import org.goobi.production.flow.jobs.AbstractGoobiJob;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExportEadFileJob extends AbstractGoobiJob {

    /**
     * When called, this method gets executed
     * 
     * It will - download the latest json file from the configured sftp server - convert it into vocabulary records - save the new records
     * 
     */

    @Override
    public void execute() {

        parseConfiguration();
        // for each configured file

        // load record from database

        // generate ead xml

        // store it in configured folder

    }

    @Override
    public String getJobName() {
        return "intranda_quartz_exportEadFile";
    }

    /**
     * Parse the configuration file
     * 
     */

    public void parseConfiguration() {
        //TODO
        // open configuration
        // check which files shall be exported
        // get folder from configuration file

    }

}
