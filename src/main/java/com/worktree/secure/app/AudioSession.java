package com.worktree.secure.app;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioSession {

    private final List<byte[]> clips = new ArrayList<>();

    public void add(byte[] wav) {
        clips.add(wav);
    }

    public byte[] mergeWavFiles() throws Exception {

        List<AudioInputStream> streams = new ArrayList<>();
        AudioFormat format = null;
        long totalFrames = 0;

        for (byte[] wav : clips) {
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(wav)
            );
            streams.add(ais);
            format = ais.getFormat();
            totalFrames += ais.getFrameLength();
        }

        AudioInputStream mergedStream =
                new AudioInputStream(
                        new SequenceInputStream(
                                Collections.enumeration(streams)
                        ),
                        format,
                        totalFrames
                );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioSystem.write(mergedStream, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    public void clear() {
        clips.clear();
    }

    public boolean isEmpty() {
        return clips.isEmpty();
    }

    public void remove(int index) {
        if (index >= 0 && index < clips.size()) {
            clips.remove(index);
        }
    }

    public int size() {
        return clips.size();
    }


}

