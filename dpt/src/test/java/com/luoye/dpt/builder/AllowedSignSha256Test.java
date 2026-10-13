package com.luoye.dpt.builder;

import org.junit.Assert;
import org.junit.Test;

public class AllowedSignSha256Test {

    private static final String UPLOAD =
            "e3208d5c1b3ede96453b8314c5a8ed7bd91c7405965d9d8121264f69ab3b9109";
    private static final String PLAY =
            "3063a90fe12458156650465a5041564a5cc9966eb68c2981e0fc39b6a462ed41";

    @Test
    public void mergesUploadCertAndPlayFingerprint() {
        String playConsole =
                "30:63:A9:0F:E1:24:58:15:66:50:46:5A:50:41:56:4A:5C:C9:96:6E:B6:8C:29:81:E0:FC:39:B6:A4:62:ED:41";
        Assert.assertEquals(UPLOAD + "," + PLAY, AndroidPackage.mergeAllowedSignSha256(UPLOAD, playConsole));
    }

    @Test
    public void keepsConfiguredCertWhenVerifySignIsOff() {
        Assert.assertEquals(PLAY, AndroidPackage.mergeAllowedSignSha256(null, PLAY));
    }

    @Test
    public void dropsDuplicateAndSha1() {
        String sha1 = "b553a9a18faf4ee93a58ba45681ca4a587f409a9";
        Assert.assertEquals(PLAY, AndroidPackage.mergeAllowedSignSha256(PLAY, PLAY + "," + sha1));
    }
}
