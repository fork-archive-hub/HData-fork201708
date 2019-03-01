package com.github.stuxuhai.hdata.plugin.reader.ftp;

import com.github.stuxuhai.hdata.api.JobConfig;
import com.github.stuxuhai.hdata.api.PluginConfig;
import com.github.stuxuhai.hdata.api.Splitter;
import com.github.stuxuhai.hdata.ftp.FtpClient;
import com.github.stuxuhai.hdata.ftp.FtpFile;
import com.github.stuxuhai.hdata.ftp.FtpOperator;
import com.github.stuxuhai.hdata.ftp.FtpsClient;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class FTPSplitter extends Splitter {

    private static final Logger LOGGER = LogManager.getLogger(FTPSplitter.class);

    @Override
    public List<PluginConfig> split(JobConfig jobConfig) {
        List<PluginConfig> list = new ArrayList<PluginConfig>();
        PluginConfig readerConfig = jobConfig.getReaderConfig();
        String host = readerConfig.getString(FTPReaderProperties.HOST);
        Preconditions.checkNotNull(host, "FTP reader required property: host");

        int port = readerConfig.getInt(FTPReaderProperties.PORT, 21);
        String username = readerConfig.getString(FTPReaderProperties.USERNAME, "anonymous");
        String password = readerConfig.getString(FTPReaderProperties.PASSWORD, "");
        String dir = readerConfig.getString(FTPReaderProperties.DIR);
        Preconditions.checkNotNull(dir, "FTP reader required property: dir");

        String filenameRegexp = readerConfig.getString(FTPReaderProperties.FILENAME);
        Preconditions.checkNotNull(filenameRegexp, "FTP reader required property: filename");

        boolean recursive = readerConfig.getBoolean(FTPReaderProperties.RECURSIVE, false);
        boolean secure = readerConfig.getBoolean(FTPReaderProperties.SECURE, false);
        int parallelism = readerConfig.getParallelism();

        FtpOperator operator = null;
        if (secure) {
            operator = new FtpsClient();
        } else {
            operator = new FtpClient();
        }
        operator.connect(host, port, username, password);
        try {
            List<FtpFile> files = new ArrayList();
            operator.list(files, dir, filenameRegexp, recursive);
            if (parallelism == 1) {
                readerConfig.put(FTPReaderProperties.FILES, files);
                list.add(readerConfig);
            } else {
                double step = (double) files.size() / parallelism;
                for (int i = 0; i < parallelism; i++) {
                    List<FtpFile> splitedFiles = new ArrayList();
                    for (int start = (int) Math.ceil(step * i), end = (int) Math
                            .ceil(step * (i + 1)); start < end; start++) {
                        splitedFiles.add(files.get(start));
                    }
                    PluginConfig pluginConfig = (PluginConfig) readerConfig.clone();
                    pluginConfig.put(FTPReaderProperties.FILES, splitedFiles);
                    list.add(pluginConfig);
                }
            }
        } catch (Exception e) {
            LOGGER.error(Throwables.getStackTraceAsString(e));
        } finally {
            operator.close();
        }
        System.out.println(list);
        return list;
    }
}
