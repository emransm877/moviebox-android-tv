package org.emransm.moviebox.tv;
import org.junit.Test;
import static org.junit.Assert.*;
public class PoliciesTest {
    @Test public void sparseEpisodesRemainSparse(){assertArrayEquals(new int[]{4},Policies.episodes("4",10));assertArrayEquals(new int[]{1,4,9},Policies.episodes("9,1,4,4,invalid",12));}
    @Test public void invalidSeeksAreBounded(){assertEquals(-1,Policies.seekPosition(40000,0));assertEquals(0,Policies.seekPosition(-10,90000));assertEquals(89750,Policies.seekPosition(99000,90000));}
    @Test public void stableNewerReleasesOnly(){assertTrue(Policies.newer("v0.2.0","0.1.0"));assertFalse(Policies.newer("0.1.0","0.1.0"));assertFalse(Policies.newer("v0.2.0-beta","0.1.0"));}
    @Test public void neverUseLockedOrWrongEpisode(){assertFalse(Policies.eligibleResource(2,5,4,5,1,0,false,1080,800000000,2400));assertFalse(Policies.eligibleResource(1,0,0,0,0,1,false,1080,800000000,2400));assertTrue(Policies.eligibleResource(2,5,4,5,4,0,false,1080,800000000,2400));}
}
