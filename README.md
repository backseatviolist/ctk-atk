SuperCollider's [Ambisonic Toolkit](http://www.ambisonictoolkit.net/) and [Composer's Toolkit](https://github.com/supercollider-quarks/Ctk) are mostly compatible, except for ATK's `FoaEncoderKernel` and `FoaDecoderKernel`. This repo offers `CtkFoaEncoderKernel` and `CtkFoaDecoderKernel`, which are (almost) drop-in replacements.

Depends on [this commit](https://github.com/supercollider/sc3-plugins/commit/9d40e985b4db3979cdb27f291eeaf51aa99c3267). Make sure your ATK is up to date.

    // New method

    (
    var score, sndbuf, sd, encoder, decoder, sampleRate;
    score = CtkScore.new;
    sampleRate = 44100;

    sndbuf = CtkBuffer.playbuf(Atk.userSoundsDir ++ "/uhj/Palestrina-O_Bone.wav");
    score.add(sndbuf);

    // The encoders and decoders can't predict the sample rate of the NRT server,
    // so you have to provide it yourself.
    encoder = CtkFoaEncoderKernel.newUHJ(sampleRate: sampleRate);
    decoder = CtkFoaDecoderKernel.newListen(1013, sampleRate: sampleRate);

    // Don't forget to add them to the score!!
    score.add(encoder);
    score.add(decoder);

    sd = CtkSynthDef(\kernelEncodeDecode, {arg buffer;
        var out, src, encode;
        src = PlayBuf.ar(2, buffer);
        encode = FoaEncode.ar(src, encoder);
        out = FoaDecode.ar(encode, decoder);
        Out.ar(0, out);
    });
    score.add(sd.note(1.0, sndbuf.duration).buffer_(sndbuf));
    score.write("~/Desktop/myDecode.wav".standardizePath,
        sampleRate: sampleRate,
        headerFormat: "WAVE",
        sampleFormat: "float",
        options: ServerOptions.new.numOutputBusChannels_(2)
        );
    // Probably not needed
    encoder.free;
    decoder.free;
    )

    // Old method

    (
    var cond, score, sndbuf, kernelInfo, sd, encoder, decoder;
    score = CtkScore.new;
    s.boot;

    // we still need to boot the Server for now to allocate ids for the kernels
    s.waitForBoot({
            sndbuf = CtkBuffer.playbuf(Atk.userSoundsDir ++ "/uhj/Palestrina-O_Bone.wav");
            score.add(sndbuf);
            encoder = FoaEncoderKernel.newUHJ;
            decoder = FoaDecoderKernel.newListen(1013);
            s.sync(cond);
            // the FoaEncoderKernel and FoaDecoderKernel classes will return info that can be
            // used to make CtkBuffers for the CtkScore. An array of [path, bufnum, channel] is
            // returned that will line up with the kernel info that the FoaEncode and FoaDecode
            // classes expect
            kernelInfo = encoder.kernelInfo ++ decoder.kernelInfo;
            kernelInfo.do({arg thisInfo;
                    var path, bufnum, channel, buf;
                    #path, bufnum, channel = thisInfo;
                    buf = CtkBuffer(path, bufnum: bufnum, channels: channel);
                    score.add(buf);
            });
            sd = CtkSynthDef(\kernelEncodeDecode, {arg buffer;
                    var out, src, encode;
                    src = PlayBuf.ar(2, buffer);
                    encode = FoaEncode.ar(src, encoder);
                    out = FoaDecode.ar(encode, decoder);
                    Out.ar(0, out);
            });
            score.add(sd.note(1.0, sndbuf.duration).buffer_(sndbuf));
            score.write("~/Desktop/myDecode.wav".standardizePath, 
                    sampleRate: 44100,
                    headerFormat: "WAVE",
                    sampleFormat: "float",
                    options: ServerOptions.new.numOutputBusChannels_(2)
                    );
            encoder.free; 
            decoder.free;
    })
    )