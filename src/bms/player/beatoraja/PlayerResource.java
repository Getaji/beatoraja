package bms.player.beatoraja;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bms.model.BMSDecoder;
import bms.model.BMSModel;
import bms.model.BMSONDecoder;
import bms.player.beatoraja.audio.AudioProcessor;
import bms.player.beatoraja.audio.SoundProcessor;
import bms.player.beatoraja.bga.BGAProcessor;
import bms.player.beatoraja.gauge.GrooveGauge;
import bms.player.beatoraja.pattern.PatternModifyLog;
import bms.player.lunaticrave2.IRScoreData;

/**
 * プレイヤーのコンポーネント間でデータをやり取りするためのクラス
 * 
 * @author exch
 */
public class PlayerResource {
	private File f;
	private BMSModel model;
	private Config config;
	private int auto;
	private AudioProcessor audio;
	private BGAProcessor bga;
	private IRScoreData score;

	private boolean finished = false;
	private boolean updateScore = true;
	private GrooveGauge grooveGauge;
	private List<Float> gauge;
	
	private String coursetitle;
	private BMSModel[] course;
	private File[] coursefile;
	private int courseindex;

	private PatternModifyLog[] pattern;

	private IRScoreData cscore;
	
	public void clear() {
		coursetitle = null;
		course = null;
		gauge = null;
		coursefile = null;
	}

	public boolean setBMSFile(final File f, final Config config, int autoplay) {
		this.config = config;
		this.auto = autoplay;
		pattern = null;
		if (f.getPath().toLowerCase().endsWith(".bmson")) {
			BMSONDecoder decoder = new BMSONDecoder(
					BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f);
			if(model.getTotal() <= 0.0) {
				model.setTotal(100.0);
			}
			int totalnotes = model.getTotalNotes();
			model.setTotal(model.getTotal() / 100.0 * 7.605 * totalnotes / (0.01 * totalnotes + 6.5));
		} else {
			BMSDecoder decoder = new BMSDecoder(BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f);
			// JUDGERANKをbmson互換に変換
			if(model.getJudgerank() < 0 || model.getJudgerank() > 2) {
				model.setJudgerank(100);
			} else {
				final int[] judgetable = {40, 70 ,90};
				model.setJudgerank(judgetable[model.getJudgerank()]);
			}
			// TOTAL未定義の場合
			if(model.getTotal() <= 0.0) {
				int totalnotes = model.getTotalNotes();
				model.setTotal(7.605 * totalnotes / (0.01 * totalnotes + 6.5));
			}
		}
		if(model.getAllTimeLines().length == 0) {
			return false;
		}
		if(this.f == null || !f.getAbsolutePath().equals(this.f.getAbsolutePath())) {
			// 前回と違うbmsファイルを読み込んだ場合はリソースのロード
			// 同フォルダの違うbmsファイルでも、WAV/,BMP定義が違う可能性があるのでロード
			this.f = f;
			this.finished = false;
			if(audio != null) {
				audio.dispose();
			}
			audio = new SoundProcessor();
			if(bga != null) {
				bga.dispose();
			}
			bga = new BGAProcessor(config);
			Thread medialoader = new Thread() {
				@Override
				public void run() {
					try {
						if (config.getBga() == Config.BGA_ON
								|| (config.getBga() == Config.BGA_AUTO && (auto != 0))) {
							bga.setModel(model, f.getPath());
						}
						audio.setModel(model, f.getPath());
					} catch (Exception e) {
						Logger.getGlobal()
								.severe(e.getClass().getName() + " : "
										+ e.getMessage());
						e.printStackTrace();
					} catch (Error e) {
						Logger.getGlobal()
								.severe(e.getClass().getName() + " : "
										+ e.getMessage());
					} finally {
						finished = true;
					}
				}
			};
			medialoader.start();				
		}
		return true;
	}

	public BMSModel getBMSModel() {
		return model;
	}

	public int getAutoplay() {
		return auto;
	}

	public Config getConfig() {
		return config;
	}

	public AudioProcessor getAudioProcessor() {
		return audio;
	}

	public BGAProcessor getBGAManager() {
		return bga;
	}

	public boolean mediaLoadFinished() {
		return finished;
	}

	public IRScoreData getScoreData() {
		return score;
	}

	public void setScoreData(IRScoreData score) {
		this.score = score;
	}

	public void setCourseBMSFiles(File[] files) {
		coursefile = files;
		List<BMSModel> models = new ArrayList();
		for (File f : files) {
			if (f.getPath().toLowerCase().endsWith(".bmson")) {
				BMSONDecoder decoder = new BMSONDecoder(
						BMSModel.LNTYPE_CHARGENOTE);
				models.add(decoder.decode(f));
			} else {
				BMSDecoder decoder = new BMSDecoder(
						BMSModel.LNTYPE_CHARGENOTE);
				models.add(decoder.decode(f));
			}
		}
		course = models.toArray(new BMSModel[0]);
	}

	public BMSModel[] getCourseBMSModels() {
		return course;
	}

	public boolean nextCourse() {
		courseindex++;
		if (courseindex == coursefile.length) {
			return false;
		} else {
			setBMSFile(coursefile[courseindex], config, auto);
			return true;
		}
	}

	public void reloadBMSFile() {
		if (f.getPath().toLowerCase().endsWith(".bmson")) {
			BMSONDecoder decoder = new BMSONDecoder(
					BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f);
		} else {
			BMSDecoder decoder = new BMSDecoder(BMSModel.LNTYPE_CHARGENOTE);
			model = decoder.decode(f);
		}
		clear();
	}

	public List<Float> getGauge() {
		return gauge;
	}

	public void setGauge(List<Float> gauge) {
		this.gauge = gauge;
	}

	public GrooveGauge getGrooveGauge() {
		return grooveGauge;
	}

	public void setGrooveGauge(GrooveGauge grooveGauge) {
		this.grooveGauge = grooveGauge;
	}

	public PatternModifyLog[] getPatternModifyLog() {
		return pattern;
	}

	public void setPatternModifyLog(PatternModifyLog[] pattern) {
		this.pattern = pattern;
	}

	public IRScoreData getCourseScoreData() {
		return cscore;
	}

	public void setCourseScoreData(IRScoreData cscore) {
		this.cscore = cscore;
	}

	public boolean isUpdateScore() {
		return updateScore;
	}
	
	public void setUpdateScore(boolean b) {
		this.updateScore = b;
	}

	public String getCoursetitle() {
		return coursetitle;
	}

	public void setCoursetitle(String coursetitle) {
		this.coursetitle = coursetitle;
	}
}