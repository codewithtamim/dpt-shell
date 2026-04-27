package com.github.codewithtamim.dpt.gradle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DptOutputNamesTest {

    @Test
    public void signedNameMatchesDptFileUtilsConvention() {
        assertEquals("app-release_signed.apk", DptOutputNames.signedArtifactName("app-release.apk"));
        assertEquals("foo.bar_signed.aab", DptOutputNames.signedArtifactName("foo.bar.aab"));
    }
}
