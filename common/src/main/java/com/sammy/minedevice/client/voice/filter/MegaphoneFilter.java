package com.sammy.minedevice.client.voice.filter;

import su.plo.voice.api.client.audio.filter.AudioFilter;
import su.plo.voice.api.client.audio.filter.AudioFilterContext;

public final class MegaphoneFilter implements AudioFilter {
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    private static final float HIGH_PASS_HZ = 1300.0F;
    private static final float LOW_PASS_HZ = 3400.0F;
    private static final float PRE_EMPHASIS = 0.91F;
    private static final float PEAK1_FREQ_HZ = 2800.0F;
    private static final float PEAK1_Q = 1.40F;
    private static final float PEAK1_GAIN_DB = 6.8F;
    private static final float PEAK2_FREQ_HZ = 3600.0F;
    private static final float PEAK2_Q = 1.60F;
    private static final float PEAK2_GAIN_DB = 4.2F;
    private static final float DRIVE_GAIN = 2.30F;
    private static final float ASYM_OFFSET = 0.08F;
    private static final float SOFT_CLIP_THRESHOLD = 0.34F;
    private static final float OUTPUT_GAIN = 1.02F;
    private static final float BIT_CRUSH_STEPS = 320.0F;
    private static final float METAL_DELAY_MS = 18.0F;
    private static final float METAL_MIX = 0.24F;
    private static final float METAL_FEEDBACK = 0.20F;
    private static final float TO_FLOAT = 1.0F / 32768.0F;

    private float sampleRate = -1.0F;
    private float alphaHighPass;
    private float alphaLowPass;
    private int delaySamples = 1;

    private float[] previousInput = new float[1];
    private float[] highPassOutput = new float[1];
    private float[] lowPassOutput = new float[1];
    private float[] preEmphasisState = new float[1];
    private float[][] delayLine = new float[][] {new float[1]};
    private int[] delayIndex = new int[] {0};
    private final Biquad peakFilterA = new Biquad();
    private final Biquad peakFilterB = new Biquad();

    @Override
    public String getName() {
        return "minedevice:megaphone";
    }

    @Override
    public short[] process(AudioFilterContext context, short[] samples) {
        if (samples.length == 0) {
            return samples;
        }

        int channels = Math.max(1, context.getChannels());
        ensureState(context, channels);

        for (int i = 0; i < samples.length; i++) {
            int channel = i % channels;
            float input = samples[i] * TO_FLOAT;
            float preEmphasis = input - preEmphasisState[channel] * PRE_EMPHASIS;
            preEmphasisState[channel] = input;

            float highPass = alphaHighPass * (highPassOutput[channel] + preEmphasis - previousInput[channel]);
            previousInput[channel] = preEmphasis;
            highPassOutput[channel] = highPass;

            float lowPass = lowPassOutput[channel] + alphaLowPass * (highPass - lowPassOutput[channel]);
            lowPassOutput[channel] = lowPass;

            float resonant = peakFilterA.process(channel, lowPass);
            resonant = peakFilterB.process(channel, resonant);

            float delayed = delayLine[channel][delayIndex[channel]];
            float metallic = resonant + delayed * METAL_MIX;
            delayLine[channel][delayIndex[channel]] = resonant + delayed * METAL_FEEDBACK;
            delayIndex[channel]++;
            if (delayIndex[channel] >= delaySamples) {
                delayIndex[channel] = 0;
            }

            float driven = metallic * DRIVE_GAIN + ASYM_OFFSET;
            float clipped = applySoftClipAsymmetric(driven, SOFT_CLIP_THRESHOLD);
            float crushed = applyBitCrush(clipped, BIT_CRUSH_STEPS);
            samples[i] = toShort(crushed * OUTPUT_GAIN);
        }

        return samples;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getSupportedChannels() {
        return 0;
    }

    private void ensureState(AudioFilterContext context, int channels) {
        if (previousInput.length != channels) {
            resetState(channels, delaySamples);
        }

        float currentSampleRate = context.getDevice().getFormat().getSampleRate();
        if (currentSampleRate <= 0.0F) {
            currentSampleRate = 48000.0F;
        }

        if (sampleRate == currentSampleRate) {
            return;
        }

        sampleRate = currentSampleRate;
        updateCoefficients();
        int newDelaySamples = Math.max(1, Math.round((sampleRate * METAL_DELAY_MS) / 1000.0F));
        resetState(channels, newDelaySamples);
    }

    private void updateCoefficients() {
        float dt = 1.0F / sampleRate;

        float highPassRc = 1.0F / (TWO_PI * HIGH_PASS_HZ);
        alphaHighPass = highPassRc / (highPassRc + dt);

        float lowPassRc = 1.0F / (TWO_PI * LOW_PASS_HZ);
        alphaLowPass = dt / (lowPassRc + dt);

        peakFilterA.configurePeak(sampleRate, PEAK1_FREQ_HZ, PEAK1_Q, PEAK1_GAIN_DB);
        peakFilterB.configurePeak(sampleRate, PEAK2_FREQ_HZ, PEAK2_Q, PEAK2_GAIN_DB);
    }

    private static float applySoftClipAsymmetric(float input, float threshold) {
        float positiveThreshold = threshold;
        float negativeThreshold = threshold * 0.92F;

        if (input >= -negativeThreshold && input <= positiveThreshold) {
            return input;
        }

        if (input > positiveThreshold) {
            float over = (input - positiveThreshold) / (1.0F - positiveThreshold);
            return positiveThreshold + (1.0F - positiveThreshold) * (float) Math.tanh(over);
        }

        float over = ((-input) - negativeThreshold) / (1.0F - negativeThreshold);
        return -negativeThreshold - (1.0F - negativeThreshold) * (float) Math.tanh(over);
    }

    private static float applyBitCrush(float input, float steps) {
        if (steps <= 0.0F) {
            return input;
        }
        return Math.round(input * steps) / steps;
    }

    private void resetState(int channels, int newDelaySamples) {
        delaySamples = Math.max(1, newDelaySamples);
        previousInput = new float[channels];
        highPassOutput = new float[channels];
        lowPassOutput = new float[channels];
        preEmphasisState = new float[channels];

        delayLine = new float[channels][];
        delayIndex = new int[channels];
        for (int i = 0; i < channels; i++) {
            delayLine[i] = new float[delaySamples];
            delayIndex[i] = 0;
        }

        peakFilterA.resize(channels);
        peakFilterB.resize(channels);
    }

    private static short toShort(float input) {
        float clamped = Math.max(-1.0F, Math.min(1.0F, input));
        return (short) Math.round(clamped * 32767.0F);
    }

    private static final class Biquad {
        private float b0 = 1.0F;
        private float b1;
        private float b2;
        private float a1;
        private float a2;

        private float[] x1 = new float[1];
        private float[] x2 = new float[1];
        private float[] y1 = new float[1];
        private float[] y2 = new float[1];

        private void resize(int channels) {
            x1 = new float[channels];
            x2 = new float[channels];
            y1 = new float[channels];
            y2 = new float[channels];
        }

        private void configurePeak(float sampleRate, float centerHz, float q, float gainDb) {
            float a = (float) Math.pow(10.0D, gainDb / 40.0D);
            float w0 = MegaphoneFilter.TWO_PI * centerHz / sampleRate;
            float cos = (float) Math.cos(w0);
            float sin = (float) Math.sin(w0);
            float alpha = sin / (2.0F * q);

            float b0n = 1.0F + alpha * a;
            float b1n = -2.0F * cos;
            float b2n = 1.0F - alpha * a;

            float a0 = 1.0F + alpha / a;
            float a1n = -2.0F * cos;
            float a2n = 1.0F - alpha / a;

            b0 = b0n / a0;
            b1 = b1n / a0;
            b2 = b2n / a0;
            a1 = a1n / a0;
            a2 = a2n / a0;
        }

        private float process(int channel, float input) {
            float output = b0 * input + b1 * x1[channel] + b2 * x2[channel] - a1 * y1[channel] - a2 * y2[channel];

            x2[channel] = x1[channel];
            x1[channel] = input;
            y2[channel] = y1[channel];
            y1[channel] = output;

            return output;
        }
    }
}
