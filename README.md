Sample:

    (
    var score, sndbuf, sd, encoder, decoder, sampleRate;
    score = CtkScore.new;
    sampleRate = 44100;

    sndbuf = CtkBuffer.playbuf(Atk.userSoundsDir ++ "/uhj/Palestrina-O_Bone.wav");
    score.add(sndbuf);
    encoder = CtkFoaEncoderKernel.newUHJ(sampleRate: sampleRate);
    decoder = CtkFoaDecoderKernel.newListen(1013, sampleRate: sampleRate);
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
    encoder.free;
    decoder.free;
    )