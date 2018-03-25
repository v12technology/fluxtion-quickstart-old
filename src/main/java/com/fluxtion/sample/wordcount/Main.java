/*
 * Copyright (C) 2016 Greg Higgins (greg.higgins@v12technology.com)
 *
 * This file is part of Fluxtion.
 *
 * Fluxtion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.fluxtion.sample.wordcount;

import com.fluxtion.sample.wordcount.generated.WcProcessor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Greg Higgins
 */
public class Main {

    public static final int SIZE = 4 * 1024;

    public static void main(String[] args) {
        File f = new File(args[0]);
        try {
            streamFromFile(f);
        } catch (IOException ex) {
            System.out.println("error processing file:" + ex.getMessage());
        }
    }

    public static WcProcessor streamFromFile(File file) throws FileNotFoundException, IOException {
        long now = System.nanoTime();
        WcProcessor processor = new WcProcessor();
        processor.init();
        if (file.exists() && file.isFile()) {
            FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
            long size = file.length();
            MappedByteBuffer buffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, 0, size);
            CharEvent charEvent = new CharEvent(' ');

            final byte[] barray = new byte[SIZE];
            int nGet;
            while (buffer.hasRemaining()) {
                nGet = Math.min(buffer.remaining(), SIZE);
                buffer.get(barray, 0, nGet);
                for (int i = 0; i < nGet; i++) {
                    charEvent.setCharacter((char) barray[i]);
                    processor.handleEvent(charEvent);
                }
            }
            processor.tearDown();
            double delta = ((int)(System.nanoTime() - now)/1_000_000)/1_000.0;
            System.out.println(processor.result.toString());
            System.out.printf("time: %.3f sec %n", delta);
        } else {
            System.out.println("cannot process file file:" + file.getAbsolutePath());
        }
        return processor;
    }
}
