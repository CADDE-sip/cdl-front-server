/*
    LogMsgクラス

    COPYRIGHT FUJITSU LIMITED 2021
*/

package com.fujitsu.cdlfrontserver.api;

public class LogMsg {

    private static final LogMsg Instance = new LogMsg();

    public static LogMsg getInstance() {
        return Instance;
    }

    public String error(String message) {
        String msg = appendPackageClassMethodErrorlevel(LogLevel.ERROR, message);
        System.out.println(msg);
        return msg;
    }

    public String warning(String message) {
        String msg = appendPackageClassMethodErrorlevel(LogLevel.WARN, message);
        System.out.println(msg);
        return msg;
    }

    public String notice(String message) {
        String msg = appendPackageClassMethodErrorlevel(LogLevel.NOTICE, message);
        System.out.println(msg);
        return msg;
    }

    public String info(String message) {
        String msg = appendPackageClassMethodErrorlevel(LogLevel.INFO, message);
        System.out.println(msg);
        return msg;
    }

    public String debug(String message) {
        String msg = appendPackageClassMethodErrorlevel(LogLevel.DEBUG, message);
        System.out.println(msg);
        return msg;
    }

    public String buildLogMsg(LogLevel loglevel, String message) {
        return appendPackageClassMethodErrorlevel(loglevel, message);
    }

    public String println(LogLevel loglevel, String message) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];

        String msg = ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] " + message;

        System.out.println(msg);

        return msg;
    }

    public String buildLogMsg(LogLevel loglevel, String message, Throwable t) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
        return ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] " + message + " : "
                + t.getMessage();
    }

    public String println(LogLevel loglevel, String message, Throwable t) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];

        String msg = ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] " + message
                + " : " + t.getMessage();

        System.out.println(msg);
        t.printStackTrace();

        return msg;
    }

    public String buildLogMsg(LogLevel loglevel, Throwable t) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];

        return ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] " + t.getMessage();
    }

    public String println(LogLevel loglevel, Throwable t) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[2];

        String msg = ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] "
                + t.getMessage();

        System.out.println(msg);
        t.printStackTrace();

        return msg;
    }

    private String appendPackageClassMethodErrorlevel(LogLevel loglevel, String message) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
        return ste.getClassName() + "." + ste.getMethodName() + "() : [" + loglevel.name() + "] " + message;
    }
}
