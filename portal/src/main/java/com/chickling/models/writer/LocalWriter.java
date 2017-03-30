package com.chickling.models.writer;

import com.chickling.bean.job.JobLog;
import com.chickling.boot.Init;
import com.chickling.face.ResultWriter;
import com.chickling.models.dfs.FSFile;
import com.chickling.models.dfs.OrcFileUtil;
import com.google.common.base.Strings;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by gl08 on 2016/9/23.
 */
public class LocalWriter implements ResultWriter {

    private Logger log=LogManager.getLogger(LocalWriter.class);

    private JobLog jobLog;

    private String exception="";


    @Override
    public void init(Object parameter) {
        HashMap map= (HashMap) parameter;
        this.jobLog= (JobLog) map.get("jobLog");
    }

    @Override
    public String getException() {
        return this.exception;
    }

    @Override
    public Integer call()   {

        String hdfstmp= Init.getCsvtmphdfsPath()+"/";
        String csvResultPath= Init.getCsvlocalPath()+"/"+jobLog.getFilepath().trim().replace(" ","").replaceAll("\\\\","").replaceAll("/+","/");
        if (!csvResultPath.startsWith("/"))
            csvResultPath="/"+csvResultPath;
        if (!csvResultPath.endsWith("/"))
            csvResultPath=csvResultPath+"/";


        if (!Strings.isNullOrEmpty(jobLog.getFilename())){
            csvResultPath=csvResultPath+jobLog.getFilename();
            hdfstmp+=jobLog.getFilename();
        }
        String sourceDir=jobLog.getJoboutput();

        if(!sourceDir.endsWith("/"))
            sourceDir=sourceDir+"/";

        OrcFileUtil orcFileUtil= OrcFileUtil.newInstance();

        String result=orcFileUtil.writeORCFilestoCSVLocal(sourceDir,hdfstmp, OrcFileUtil.TYPE.HDFS, FSFile.FSType.HDFS);

        log.info("tmp csv file Path is "+result);
        if(!Strings.isNullOrEmpty(result)) {
            FSFile fsFile= FSFile.newInstance(FSFile.FSType.HDFS);
            try {
                log.info("check parent Dir is Exist");
                String targetPath=csvResultPath+result.split(jobLog.getFilename())[1];
                File file=new File(targetPath);
                if (! file.getParentFile().exists()){
                    log.info("Parent Dir not Exist !! mkdirs : [ "+file.getParent()+"  ]");
                    file.getParentFile().mkdirs();
                }
                fsFile.moveFSFileToLocal(result,targetPath);
                log.info("Save Orc File to [Local] CSV File Success !!! ");
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
                this.exception= ExceptionUtils.getStackTrace(e);
            }
        }else{
            log.error("Save CSV File Error");
            return 0;
        }
        return 2;
    }



}