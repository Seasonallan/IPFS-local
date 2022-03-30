package com.season.ipfs_local;

import org.junit.Test;

import java.net.URLDecoder;
import java.net.URLEncoder;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        String json = "{你好？  中国}";
        String data = URLEncoder.encode(json);
        System.out.println(json);
        System.out.println(data);
        System.out.println(URLDecoder.decode(data));
        assertEquals(4, 2 + 2);
    }
}