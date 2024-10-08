package edu.jhuapl.sbmt.model.bennu.spectra.otes;

import java.io.IOException;

import edu.jhuapl.sbmt.core.body.ISmallBodyModel;
import edu.jhuapl.sbmt.query.v2.IDataQuery;
import edu.jhuapl.sbmt.spectrum.model.core.BasicSpectrumInstrument;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraType;
import edu.jhuapl.sbmt.spectrum.model.core.SpectraTypeFactory;
import edu.jhuapl.sbmt.spectrum.model.core.SpectrumInstrumentFactory;
import edu.jhuapl.sbmt.spectrum.model.core.search.SpectraHierarchicalSearchSpecification;
import edu.jhuapl.sbmt.spectrum.model.io.SpectrumInstrumentMetadataIO;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.Spectrum;
import edu.jhuapl.sbmt.spectrum.model.sbmtCore.spectra.math.SpectrumMath;
import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

/**
 * Representing information about an OTES spectra, including the default coloring max values and indices, units, query and spectrum math types
 * @author steelrj1
 *
 */
public class OTES extends BasicSpectrumInstrument
{
    public static int bandCentersLength = 348;

    static
    {
        SpectrumInstrumentFactory.registerType("OTES", new OTES());
    }

    @Override
    public Spectrum getSpectrumInstance(String filename,
            ISmallBodyModel smallBodyModel, SpectraHierarchicalSearchSpecification searchSpec)
            throws IOException
    {
        return new OTESSpectrum(filename, (SpectrumInstrumentMetadataIO)searchSpec, smallBodyModel.getBoundingBoxDiagonalLength(), this);
    }

	@Override
	public double[] getRGBMaxVals()
	{
		return new double[] {0.000007, 0.000007, 0.000007};
	}

	@Override
	public int[] getRGBDefaultIndices()
	{
		return new int[] {50, 100, 150};
	}

	@Override
	public String[] getDataTypeNames()
	{
		return new String[] {"L2", "L3"};
	}

    public OTES()
    {
         super("cm^-1", "OTES", OTESQuery.getInstance(), OTESSpectrumMath.getInstance());
         bandCenters = new Double[]{ 8.660700e+00, // 0
                 1.732140e+01, // 1
                 2.598210e+01, // 2
                 3.464280e+01, // 3
                 4.330350e+01, // 4
                 5.196420e+01, // 5
                 6.062490e+01, // 6
                 6.928560e+01, // 7
                 7.794630e+01, // 8
                 8.660700e+01, // 9
                 9.526770e+01, // 10
                 1.039285e+02, // 11
                 1.125892e+02, // 12
                 1.212499e+02, // 13
                 1.299106e+02, // 14
                 1.385713e+02, // 15
                 1.472320e+02, // 16
                 1.558927e+02, // 17
                 1.645534e+02, // 18
                 1.732141e+02, // 19
                 1.818748e+02, // 20
                 1.905355e+02, // 21
                 1.991962e+02, // 22
                 2.078570e+02, // 23
                 2.165177e+02, // 24
                 2.251784e+02, // 25
                 2.338391e+02, // 26
                 2.424998e+02, // 27
                 2.511605e+02, // 28
                 2.598212e+02, // 29
                 2.684819e+02, // 30
                 2.771426e+02, // 31
                 2.858033e+02, // 32
                 2.944640e+02, // 33
                 3.031247e+02, // 34
                 3.117855e+02, // 35
                 3.204462e+02, // 36
                 3.291069e+02, // 37
                 3.377676e+02, // 38
                 3.464283e+02, // 39
                 3.550890e+02, // 40
                 3.637497e+02, // 41
                 3.724104e+02, // 42
                 3.810711e+02, // 43
                 3.897318e+02, // 44
                 3.983925e+02, // 45
                 4.070532e+02, // 46
                 4.157140e+02, // 47
                 4.243747e+02, // 48
                 4.330354e+02, // 49
                 4.416961e+02, // 50
                 4.503568e+02, // 51
                 4.590175e+02, // 52
                 4.676782e+02, // 53
                 4.763389e+02, // 54
                 4.849996e+02, // 55
                 4.936603e+02, // 56
                 5.023211e+02, // 57
                 5.109818e+02, // 58
                 5.196425e+02, // 59
                 5.283032e+02, // 60
                 5.369639e+02, // 61
                 5.456246e+02, // 62
                 5.542853e+02, // 63
                 5.629460e+02, // 64
                 5.716067e+02, // 65
                 5.802674e+02, // 66
                 5.889281e+02, // 67
                 5.975888e+02, // 68
                 6.062495e+02, // 69
                 6.149103e+02, // 70
                 6.235710e+02, // 71
                 6.322317e+02, // 72
                 6.408924e+02, // 73
                 6.495531e+02, // 74
                 6.582138e+02, // 75
                 6.668745e+02, // 76
                 6.755353e+02, // 77
                 6.841960e+02, // 78
                 6.928567e+02, // 79
                 7.015174e+02, // 80
                 7.101781e+02, // 81
                 7.188388e+02, // 82
                 7.274995e+02, // 83
                 7.361602e+02, // 84
                 7.448209e+02, // 85
                 7.534816e+02, // 86
                 7.621423e+02, // 87
                 7.708030e+02, // 88
                 7.794637e+02, // 89
                 7.881244e+02, // 90
                 7.967851e+02, // 91
                 8.054458e+02, // 92
                 8.141065e+02, // 93
                 8.227672e+02, // 94
                 8.314280e+02, // 95
                 8.400887e+02, // 96
                 8.487495e+02, // 97
                 8.574102e+02, // 98
                 8.660709e+02, // 99
                 8.747316e+02, // 100
                 8.833923e+02, // 101
                 8.920530e+02, // 102
                 9.007137e+02, // 103
                 9.093744e+02, // 104
                 9.180351e+02, // 105
                 9.266958e+02, // 106
                 9.353565e+02, // 107
                 9.440172e+02, // 108
                 9.526779e+02, // 109
                 9.613386e+02, // 110
                 9.699993e+02, // 111
                 9.786600e+02, // 112
                 9.873207e+02, // 113
                 9.959814e+02, // 114
                 1.004642e+03, // 115
                 1.013303e+03, // 116
                 1.021964e+03, // 117
                 1.030624e+03, // 118
                 1.039285e+03, // 119
                 1.047946e+03, // 120
                 1.056606e+03, // 121
                 1.065267e+03, // 122
                 1.073928e+03, // 123
                 1.082589e+03, // 124
                 1.091249e+03, // 125
                 1.099910e+03, // 126
                 1.108571e+03, // 127
                 1.117231e+03, // 128
                 1.125892e+03, // 129
                 1.134553e+03, // 130
                 1.143214e+03, // 131
                 1.151874e+03, // 132
                 1.160535e+03, // 133
                 1.169196e+03, // 134
                 1.177856e+03, // 135
                 1.186517e+03, // 136
                 1.195178e+03, // 137
                 1.203839e+03, // 138
                 1.212499e+03, // 139
                 1.221160e+03, // 140
                 1.229821e+03, // 141
                 1.238481e+03, // 142
                 1.247142e+03, // 143
                 1.255803e+03, // 144
                 1.264463e+03, // 145
                 1.273124e+03, // 146
                 1.281785e+03, // 147
                 1.290446e+03, // 148
                 1.299106e+03, // 149
                 1.307767e+03, // 150
                 1.316428e+03, // 151
                 1.325088e+03, // 152
                 1.333749e+03, // 153
                 1.342410e+03, // 154
                 1.351071e+03, // 155
                 1.359731e+03, // 156
                 1.368392e+03, // 157
                 1.377053e+03, // 158
                 1.385713e+03, // 159
                 1.394374e+03, // 160
                 1.403035e+03, // 161
                 1.411695e+03, // 162
                 1.420356e+03, // 163
                 1.429017e+03, // 164
                 1.437678e+03, // 165
                 1.446338e+03, // 166
                 1.454999e+03, // 167
                 1.463660e+03, // 168
                 1.472320e+03, // 169
                 1.480981e+03, // 170
                 1.489642e+03, // 171
                 1.498303e+03, // 172
                 1.506963e+03, // 173
                 1.515624e+03, // 174
                 1.524285e+03, // 175
                 1.532945e+03, // 176
                 1.541606e+03, // 177
                 1.550267e+03, // 178
                 1.558927e+03, // 179
                 1.567588e+03, // 180
                 1.576249e+03, // 181
                 1.584910e+03, // 182
                 1.593570e+03, // 183
                 1.602231e+03, // 184
                 1.610892e+03, // 185
                 1.619552e+03, // 186
                 1.628213e+03, // 187
                 1.636874e+03, // 188
                 1.645535e+03, // 189
                 1.654195e+03, // 190
                 1.662856e+03, // 191
                 1.671517e+03, // 192
                 1.680177e+03, // 193
                 1.688838e+03, // 194
                 1.697499e+03, // 195
                 1.706160e+03, // 196
                 1.714820e+03, // 197
                 1.723481e+03, // 198
                 1.732142e+03, // 199
                 1.740802e+03, // 200
                 1.749463e+03, // 201
                 1.758124e+03, // 202
                 1.766785e+03, // 203
                 1.775445e+03, // 204
                 1.784106e+03, // 205
                 1.792767e+03, // 206
                 1.801427e+03, // 207
                 1.810088e+03, // 208
                 1.818749e+03, // 209
                 1.827409e+03, // 210
                 1.836070e+03, // 211
                 1.844731e+03, // 212
                 1.853392e+03, // 213
                 1.862052e+03, // 214
                 1.870713e+03, // 215
                 1.879374e+03, // 216
                 1.888034e+03, // 217
                 1.896695e+03, // 218
                 1.905356e+03, // 219
                 1.914017e+03, // 220
                 1.922677e+03, // 221
                 1.931338e+03, // 222
                 1.939999e+03, // 223
                 1.948659e+03, // 224
                 1.957320e+03, // 225
                 1.965981e+03, // 226
                 1.974641e+03, // 227
                 1.983302e+03, // 228
                 1.991963e+03, // 229
                 2.000624e+03, // 230
                 2.009284e+03, // 231
                 2.017945e+03, // 232
                 2.026606e+03, // 233
                 2.035266e+03, // 234
                 2.043927e+03, // 235
                 2.052588e+03, // 236
                 2.061249e+03, // 237
                 2.069909e+03, // 238
                 2.078570e+03, // 239
                 2.087231e+03, // 240
                 2.095891e+03, // 241
                 2.104552e+03, // 242
                 2.113213e+03, // 243
                 2.121874e+03, // 244
                 2.130534e+03, // 245
                 2.139195e+03, // 246
                 2.147856e+03, // 247
                 2.156516e+03, // 248
                 2.165177e+03, // 249
                 2.173838e+03, // 250
                 2.182499e+03, // 251
                 2.191159e+03, // 252
                 2.199820e+03, // 253
                 2.208481e+03, // 254
                 2.217141e+03, // 255
                 2.225802e+03, // 256
                 2.234463e+03, // 257
                 2.243124e+03, // 258
                 2.251784e+03, // 259
                 2.260445e+03, // 260
                 2.269106e+03, // 261
                 2.277766e+03, // 262
                 2.286427e+03, // 263
                 2.295088e+03, // 264
                 2.303749e+03, // 265
                 2.312409e+03, // 266
                 2.321070e+03, // 267
                 2.329730e+03, // 268
                 2.338391e+03, // 269
                 2.347052e+03, // 270
                 2.355713e+03, // 271
                 2.364373e+03, // 272
                 2.373034e+03, // 273
                 2.381695e+03, // 274
                 2.390355e+03, // 275
                 2.399016e+03, // 276
                 2.407677e+03, // 277
                 2.416338e+03, // 278
                 2.424998e+03, // 279
                 2.433659e+03, // 280
                 2.442320e+03, // 281
                 2.450980e+03, // 282
                 2.459641e+03, // 283
                 2.468302e+03, // 284
                 2.476963e+03, // 285
                 2.485623e+03, // 286
                 2.494284e+03, // 287
                 2.502945e+03, // 288
                 2.511605e+03, // 289
                 2.520266e+03, // 290
                 2.528927e+03, // 291
                 2.537588e+03, // 292
                 2.546248e+03, // 293
                 2.554909e+03, // 294
                 2.563570e+03, // 295
                 2.572230e+03, // 296
                 2.580891e+03, // 297
                 2.589552e+03, // 298
                 2.598213e+03, // 299
                 2.606873e+03, // 300
                 2.615534e+03, // 301
                 2.624195e+03, // 302
                 2.632855e+03, // 303
                 2.641516e+03, // 304
                 2.650177e+03, // 305
                 2.658837e+03, // 306
                 2.667498e+03, // 307
                 2.676159e+03, // 308
                 2.684820e+03, // 309
                 2.693480e+03, // 310
                 2.702141e+03, // 311
                 2.710802e+03, // 312
                 2.719462e+03, // 313
                 2.728123e+03, // 314
                 2.736784e+03, // 315
                 2.745445e+03, // 316
                 2.754105e+03, // 317
                 2.762766e+03, // 318
                 2.771427e+03, // 319
                 2.780087e+03, // 320
                 2.788748e+03, // 321
                 2.797409e+03, // 322
                 2.806070e+03, // 323
                 2.814730e+03, // 324
                 2.823391e+03, // 325
                 2.832052e+03, // 326
                 2.840712e+03, // 327
                 2.849373e+03, // 328
                 2.858034e+03, // 329
                 2.866695e+03, // 330
                 2.875355e+03, // 331
                 2.884016e+03, // 332
                 2.892677e+03, // 333
                 2.901337e+03, // 334
                 2.909998e+03, // 335
                 2.918659e+03, // 336
                 2.927319e+03, // 337
                 2.935980e+03, // 338
                 2.944641e+03, // 339
                 2.953302e+03, // 340
                 2.961962e+03, // 341
                 2.970623e+03, // 342
                 2.979284e+03, // 343
                 2.987944e+03, // 344
                 2.996605e+03, // 345
                 3.005266e+03, // 346
                 3.013927e+03, // 347
                 3.022587e+03 // 348
         };
    }

    //metadata interface
    private static final Key<OTES> OTES_KEY = Key.of("otes");
    private static final Key<String> spectraNameKey = Key.of("displayName");

    public static void initializeSerializationProxy()
	{
		InstanceGetter.defaultInstanceGetter().register(OTES_KEY, (metadata) -> {

			OTES inst = null;
			String displayName = metadata.get(spectraNameKey);
			SpectraType spectraType = SpectraTypeFactory.findSpectraTypeForDisplayName(displayName);

			IDataQuery queryBase = spectraType.getQueryBase();
			SpectrumMath spectrumMath = spectraType.getSpectrumMath();
			Double[] bandCenters = spectraType.getBandCenters();
			String bandCenterUnit = spectraType.getBandCenterUnit();

			inst = new OTES();
			inst.bandCenterUnit = bandCenterUnit;
			inst.displayName = displayName;
			inst.queryBase = queryBase;
			inst.spectrumMath = spectrumMath;
			inst.bandCenters = bandCenters;

			return inst;
		},
	    OTES.class,
	    key -> {
			 SettableMetadata metadata = SettableMetadata.of(Version.of(1, 0));
			 metadata.put(spectraNameKey, key.getDisplayName());
			 return metadata;
		});
	}
}
