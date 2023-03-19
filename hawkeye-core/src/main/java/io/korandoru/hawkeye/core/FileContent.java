/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.korandoru.hawkeye.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

/**
 * A wrapper class of {@link File} and its content.
 */
public class FileContent {

    static boolean matchAntPathPattern( String[] patDirs, String[] strDirs, boolean isCaseSensitive )
    {
        int patIdxStart = 0;
        int patIdxEnd = patDirs.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.length - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = patDirs[patIdxStart];
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, strDirs[strIdxStart], isCaseSensitive ) )
            {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs[i].equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            if ( patIdxStart > patIdxEnd )
            {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = patDirs[patIdxEnd];
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, strDirs[strIdxEnd], isCaseSensitive ) )
            {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs[i].equals( "**" ) )
                {
                    return false;
                }
            }
            return true;
        }

        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patDirs[i].equals( "**" ) )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
            for ( int i = 0; i <= strLength - patLength; i++ )
            {
                for ( int j = 0; j < patLength; j++ )
                {
                    String subPat = patDirs[patIdxStart + j + 1];
                    String subStr = strDirs[strIdxStart + i + j];
                    if ( !match( subPat, subStr, isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if ( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( !patDirs[i].equals( "**" ) )
            {
                return false;
            }
        }

        return true;
    }

    public static boolean match( String pattern, String str, boolean isCaseSensitive )
    {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for ( char aPatArr : patArr )
        {
            if ( aPatArr == '*' )
            {
                containsStar = true;
                break;
            }
        }

        if ( !containsStar )
        {
            // No '*'s, so we make a shortcut
            if ( patIdxEnd != strIdxEnd )
            {
                return false; // Pattern and string do not have the same size
            }
            for ( int i = 0; i <= patIdxEnd; i++ )
            {
                ch = patArr[i];
                if ( ch != '?' && !equals( ch, strArr[i], isCaseSensitive ) )
                {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if ( patIdxEnd == 0 )
        {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        // CHECKSTYLE_OFF: InnerAssignment
        while ( ( ch = patArr[patIdxStart] ) != '*' && strIdxStart <= strIdxEnd )
        // CHECKSTYLE_ON: InnerAssignment
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxStart], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        // CHECKSTYLE_OFF: InnerAssignment
        while ( ( ch = patArr[patIdxEnd] ) != '*' && strIdxStart <= strIdxEnd )
        // CHECKSTYLE_ON: InnerAssignment
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxEnd], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] == '*' )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
            for ( int i = 0; i <= strLength - patLength; i++ )
            {
                for ( int j = 0; j < patLength; j++ )
                {
                    ch = patArr[patIdxStart + j + 1];
                    if ( ch != '?' && !equals( ch, strArr[strIdxStart + i + j], isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if ( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( patArr[i] != '*' )
            {
                return false;
            }
        }
        return true;
    }

    private static boolean equals( char c1, char c2, boolean isCaseSensitive )
    {
        if ( c1 == c2 )
        {
            return true;
        }
        if ( !isCaseSensitive )
        {
            // NOTE: Try both upper case and lower case as done by String.equalsIgnoreCase()
            if ( Character.toUpperCase( c1 ) == Character.toUpperCase( c2 )
                    || Character.toLowerCase( c1 ) == Character.toLowerCase( c2 ) )
            {
                return true;
            }
        }
        return false;
    }

    private final File file;
    private final StringBuilder fileContent;
    private int oldPos;
    private int position;

    public FileContent(File file) {
        this(file, StandardCharsets.UTF_8);
    }

    public FileContent(File file, Charset charset) {
        this.file = file;
        try {
            this.fileContent = new StringBuilder(IOUtils.toString(file.toURI(), charset));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file " + file, e);
        }
    }

    public void resetTo(int pos) {
        oldPos = position;
        position = pos;
    }

    public void reset() {
        oldPos = position;
        position = 0;
    }

    public void rewind() {
        position = oldPos;
    }

    public boolean endReached() {
        return position >= fileContent.length();
    }

    public String nextLine() {
        if (endReached()) {
            return null;
        }
        int lf = fileContent.indexOf("\n", position);
        int eol = lf == -1 || lf == 0 ? fileContent.length() : fileContent.charAt(lf - 1) == '\r' ? lf - 1 : lf;
        String str = fileContent.substring(position, eol);
        oldPos = position;
        position = lf == -1 ? fileContent.length() : lf + 1;
        return str;
    }

    public int getPosition() {
        return position;
    }

    public void delete(int start, int end) {
        fileContent.delete(start, end);
    }

    public void insert(int index, String str) {
        fileContent.insert(index, str);
    }

    public void removeDuplicatedEmptyEndLines() {
        int pos;
        while ((pos = fileContent.lastIndexOf("\n")) != -1) {
            boolean cr = false;
            if (pos > 0 && fileContent.charAt(pos - 1) == '\r') {
                cr = true;
                pos--;
            }
            if (pos > 0 && fileContent.charAt(pos - 1) == '\n') {
                fileContent.deleteCharAt(pos);
                if (cr) {
                    fileContent.deleteCharAt(pos);
                }
            } else {
                break;
            }
        }
        oldPos = position;
        position = fileContent.length();
    }

    public String getContent() {
        return fileContent.toString();
    }

    @Override
    public String toString() {
        return file.toString();
    }
}
