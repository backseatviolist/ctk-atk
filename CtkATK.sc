/*
	Copyright the ATK Community, Joseph Anderson, and Nathan Ho, 2015
		J Anderson	j.anderson[at]ambisonictoolkit.net
		Nathan Ho	nathan[at]snappizz.com

	This file is derived from the SuperCollider3 version of the Ambisonic Toolkit (ATK).

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is free software:
	you can redistribute it and/or modify it under the terms of the GNU General
	Public License as published by the Free Software Foundation, either version 3
	of the License, or (at your option) any later version.

	The SuperCollider3 version of the Ambisonic Toolkit (ATK) is distributed in
	the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
	implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
	the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with the
	SuperCollider3 version of the Ambisonic Toolkit (ATK). If not, see
	<http://www.gnu.org/licenses/>.
*/

//	This file is a bridge between the SuperCollider3 version of the Ambisonic Toolkit
//	(ATK) and the Composer's Toolkit (CTK).
//
//	ATK and CTK get along fine except for kernel convolution. ATK uses the
//	standard Buffer object, which requires you to boot a server and convert all
//	the kernel buffers into CtkBuffers.
//
//	These are alternatives to FoaEncoderKernel and FoaDecoderKernel that bypass
//	this issue by using CtkBuffers.
//
//	Class: CtkFoaDecoderKernel
//	Class: CtkFoaEncoderKernel
//
//	For more information on the ATK, visit http://ambisonictoolkit.net/ or
//	email info[at]ambisonictoolkit.net
//

// sclang does not support diamond inheritance, so we need to choose between
// inheriting from FoaEncoderKernel/FoaDecoderKernel and CtkObj.
// The former seems like a superior choice since the CtkFoa*Kernels don't behave
// much like Ctk objects aside from containing CtkBuffers and being addable to
// scores.

CtkFoaDecoderKernel : FoaDecoderKernel {

	*newSpherical { arg subjectID = 0004, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('spherical', subjectID).initKernel(nil, sampleRate);
	}

	*newListen { arg subjectID = 1002, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('listen', subjectID).initKernel(nil, sampleRate);
	}

	*newCIPIC { arg subjectID = 0021, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('cipic', subjectID).initKernel(nil, sampleRate);
	}

	*newUHJ { arg kernelSize = 512, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('uhj', 0).initKernel(kernelSize, sampleRate);
	}

	addTo {arg aCtkScore;
		aCtkScore.add(this);
		^this;
		}

	initKernel { arg kernelSize, argSampleRate;

		var databasePath, subjectPath;
		var chans;
		var sampleRate;
		var errorMsg;

		kernelBundle = nil;
		kernelInfo = [];

		// constants
		chans = 2;			// stereo kernel

		// init dirChannels (output channel (speaker) directions) and kernel sr
		if ( kind == 'uhj', {
		    dirChannels = [ pi/6, pi.neg/6 ];
			sampleRate = "None";
		}, {
			dirChannels = [ 5/9 * pi, 5/9 * pi.neg ];
			sampleRate = argSampleRate.asString;
		});

		// init kernelSize if need be (usually for HRIRs)
		if ( kernelSize == nil, {
			kernelSize = Dictionary.newFrom([
				'None', 512,
				'44100', 512,
				'48000', 512,
				'88200', 1024,
				'96000', 1024,
				'192000', 2048,
			]).at(sampleRate.asSymbol)
		});


		// init kernel root, generate subjectPath and kernelFiles
		databasePath = this.initPath;

		subjectPath = databasePath +/+ PathName.new(
			sampleRate ++ "/" ++
			kernelSize ++ "/" ++
			subjectID.asString.padLeft(4, "0")
		);

		if ( subjectPath.isFolder.not, {	// does kernel path exist?

			case
			// --> missing kernel database
				{ databasePath.isFolder.not }
				{
					errorMsg = "ATK kernel database missing!" +
						"Please install % database.".format(kind)
				}

			// --> unsupported SR
				{ PathName.new(subjectPath.parentLevelPath(2)).isFolder.not }
				{
					"Supported samplerates:".warn;
					PathName.new(subjectPath.parentLevelPath(3)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Samplerate = % is not available for".format(sampleRate)
						+
						"% kernel decoder.".format(kind)
				}

			// --> unsupported kernelSize
				{ PathName.new(subjectPath.parentLevelPath(1)).isFolder.not }
				{
					"Supported kernel sizes:".warn;
					PathName.new(subjectPath.parentLevelPath(2)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Kernel size = % is not available for".format(kernelSize)
					+
					"% kernel decoder.".format(kind)
				}

			// --> unsupported subject
				{ subjectPath.isFolder.not }
				{
					"Supported subjects:".warn;
					PathName.new(subjectPath.parentLevelPath(1)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Subject % is not available for".format(subjectID)
					+
					"% kernel decoder.".format(kind)
				};

			// throw error!
			"\n".post;
			Error(errorMsg).throw
		}, {
			// Else... everything is fine! Load kernel.
			kernel = subjectPath.files.collect({ arg kernelPath;
				chans.collect({ arg chan;
					var buf = CtkBuffer(kernelPath.fullPath, channels: [chan]);
					kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
//					"Kernel %, channel % loaded.".format(kernelPath.fileName, chan).postln;
					buf;
				})
			})
		})
	}
}

CtkFoaEncoderKernel : FoaEncoderKernel {

	*newUHJ { arg kernelSize = nil, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('uhj', 0).initKernel(kernelSize, sampleRate);
	}

	*newSuper { arg kernelSize = nil, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('super', 0).initKernel(kernelSize, sampleRate);
	}

	*newSpread { arg subjectID = 0006, kernelSize = 2048, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('spread', subjectID).initKernel(kernelSize, sampleRate);
	}

	*newDiffuse { arg subjectID = 0003, kernelSize = 2048, sampleRate = Server.default.sampleRate;
		^super.newCopyArgs('diffuse', subjectID).initKernel(kernelSize, sampleRate);
	}

	addTo {arg aCtkScore;
		aCtkScore.add(this);
		^this;
		}

	initKernel { arg kernelSize, argSampleRate;

		var databasePath, subjectPath;
		var chans;
		var sampleRate;
		var errorMsg;

		kernelBundle = nil;
		kernelInfo = [];

		// init dirChannels (output channel (speaker) directions) and kernel sr
		switch ( kind,
			'super', {
				dirChannels = [ pi/4, pi.neg/4 ];	 // approx, doesn't include phasiness
				sampleRate = "None";
				chans = 3;					// [w, x, y]
			},
			'uhj', {
				dirChannels = [ inf, inf ];
				sampleRate = argSampleRate.asString;
				chans = 3;					// [w, x, y]
			},
			'spread', {
				dirChannels = [ inf ];
				sampleRate = argSampleRate.asString;
				chans = 4;					// [w, x, y, z]
			},
			'diffuse', {
				dirChannels = [ inf ];
				sampleRate = "None";
				chans = 4;					// [w, x, y, z]
			}
		);

		// init kernelSize if need be
		if ( kernelSize == nil, {
			kernelSize = Dictionary.newFrom([
				'None', 512,
				'44100', 512,
				'48000', 512,
				'88200', 1024,
				'96000', 1024,
				'192000', 2048,
			]).at(sampleRate.asSymbol)
		});


		// init kernel root, generate subjectPath and kernelFiles
		databasePath = this.initPath;

		subjectPath = databasePath +/+ PathName.new(
			sampleRate ++ "/" ++
			kernelSize ++ "/" ++
			subjectID.asString.padLeft(4, "0")
		);


		if ( subjectPath.isFolder.not, {	// does kernel path exist?

			case
			// --> missing kernel database
				{ databasePath.isFolder.not }
				{
					errorMsg = "ATK kernel database missing!" +
						"Please install % database.".format(kind)
				}

			// --> unsupported SR
				{ PathName.new(subjectPath.parentLevelPath(2)).isFolder.not }
				{
					"Supported samplerates:".warn;
					PathName.new(subjectPath.parentLevelPath(3)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Samplerate = % is not available for".format(sampleRate)
						+
						"% kernel encoder.".format(kind)
				}

			// --> unsupported kernelSize
				{ PathName.new(subjectPath.parentLevelPath(1)).isFolder.not }
				{
					"Supported kernel sizes:".warn;
					PathName.new(subjectPath.parentLevelPath(2)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Kernel size = % is not available for".format(kernelSize)
					+
					"% kernel encoder.".format(kind)
				}

			// --> unsupported subject
				{ subjectPath.isFolder.not }
				{
					"Supported subjects:".warn;
					PathName.new(subjectPath.parentLevelPath(1)).folders.do({
						arg folder;
						("\t" + folder.folderName).postln;
				});

					errorMsg = "Subject % is not available for".format(subjectID)
					+
					"% kernel encoder.".format(kind)
				};

			// throw error!
			"\n".post;
			Error(errorMsg).throw
		}, {
			// Else... everything is fine! Load kernel.
			kernel = subjectPath.files.collect({ arg kernelPath;
				chans.collect({ arg chan;
					var buf = CtkBuffer(kernelPath.fullPath, channels: [chan]);
					kernelInfo = kernelInfo.add([kernelPath.fullPath, buf.bufnum, [chan]]);
//					"Kernel %, channel % loaded.".format(kernelPath.fileName, chan).postln;
					buf;
				})
			})
		})
	}
}

// Make CtkScore accept these classes

+ CtkScore {
	add {arg ... events;
		events.flat.do({arg event;
			case { // if the event is a note ...
				event.isKindOf(CtkNote)
				} {
				notes = notes.add(event);
				this.checkEndTime(event);
				} {
				event.isKindOf(CtkGroup);
				} {
				groups = groups.add(event);
				this.checkEndTime(event);
				} { // if the event is a buffer
				event.isKindOf(CtkBuffer);
				} {
				buffersScored.if({buffersScored = false});
				buffers = buffers.add(event);
				} {
				event.isKindOf(CtkEvent);
				} {
				ctkevents = ctkevents.add(event);
				this.checkEndTime(event);
				} {
				event.isKindOf(CtkControl);
				} {
				event.isScored.not.if({
					controls = controls.add(event);
					event.isScored = true;
					event.ctkNote.notNil.if({
						this.add(event.ctkNote);
						});
					this.checkEndTime(event);
					})
				} {
				event.isKindOf(CtkAudio);
				} {
				// do nothing, but don't complain either!
				} {
				event.isKindOf(CtkScore);
				} {
				ctkscores = ctkscores.add(event);
				buffers = buffers.addAll(event.buffers);
				this.checkEndTime(event);
				} {
				event.isKindOf(CtkMsg);
				} {
				messages = messages.add(event);
				this.checkEndTime(event);
				} {
				(event.isKindOf(CtkSynthDef) or: {event.isKindOf(CtkNoteObject)})
				} {
				sds = sds.add(event);
				} {
				event.isKindOf(CtkProtoNotes)
				} {
				event.synthdefs.do({arg thisSD;
					sds = sds.add(thisSD);
				})
				} {
				event.isKindOf(CtkFoaEncoderKernel) or: {event.isKindOf(CtkFoaDecoderKernel)}
				} {
				buffers = buffers.addAll(event.kernel.flat);
				} {
				event.respondsTo(\messages);
				} {
				others = others.add(event);
				event.respondsTo(\endtime).if({
					this.checkEndTime(event)
					})
				} {
				true
				} {
				"It appears that you are trying to add a non-Ctk object to this score".warn;
				}
			});
			oscready.if({this.clearOSC});
		}
}

// Make FoaEncode and FoaDecode accept these classes

+ FoaDecode {
	*ar { arg in, decoder, mul = 1, add = 0;
		in = this.checkChans(in);

		case
			{ decoder.isKindOf(FoaDecoderMatrix) } {

				if ( decoder.shelfFreq.isNumber, { // shelf filter?
					in = FoaPsychoShelf.ar(in,
						decoder.shelfFreq, decoder.shelfK.at(0), decoder.shelfK.at(1))
				});

				^AtkMatrixMix.ar(in, decoder.matrix, mul, add)
			}
			
			{ decoder.isKindOf(FoaDecoderKernel) } {
				^AtkKernelConv.ar(in, decoder.kernel, mul, add)
			};
	}
}

+ FoaEncode {
	*ar { arg in, encoder, mul = 1, add = 0;
		
		var out;

		case 
			{ encoder.isKindOf(FoaEncoderMatrix) } {
				out = AtkMatrixMix.ar(in, encoder.matrix, mul, add)
			}
			
			{ encoder.isKindOf(FoaEncoderKernel) } {
				out = AtkKernelConv.ar(in, encoder.kernel, mul, add)
			};

		out = this.checkChans(out);
		^out
	}
}