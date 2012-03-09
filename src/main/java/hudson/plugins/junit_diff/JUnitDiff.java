package hudson.plugins.junit_diff;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.DescribableList;

import java.io.File;
import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class JUnitDiff extends Recorder {
	
	public static final String DATA_DIR = "junitDiffArchive";
	private String junitData;
	
	@DataBoundConstructor
	public JUnitDiff() {
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	public String getJunitData(){
		return junitData;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		System.out.println("JUnit diff...");
		DescribableList<Publisher,Descriptor<Publisher>> pubList = build.getParent().getPublishersList();
		for(Publisher pub:pubList){
			if(pub instanceof JUnitResultArchiver){
				junitData = ((JUnitResultArchiver)pub).getTestResults();
			}
		}
		//copy test results
		if(junitData!=null){
			String dataExp = build.getEnvironment(listener).expand(junitData);
			int nArtif = build.getWorkspace().copyRecursiveTo(dataExp,"",new FilePath(new File(build.getRootDir(),DATA_DIR)));
			if(nArtif == 0){
				//TODO some error handling
				return false;
			}
		}
		else{
			//TODO some error handling
			return false;
		}
		//build.addAction(new JUnitDiffAction(build.getDisplayName(), build.getRootDir() + "junitResult.xml"));
		build.addAction(new JUnitDiffAction(build));
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor(){
		return (DescriptorImpl)super.getDescriptor();
	}
	

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>{

		private String junitDiffPath;
		
		public DescriptorImpl() {
            super(JUnitDiff.class);
            load();
        }
		
		public String getDisplayName() {
			return "JUnit Diff";
		}

		public String getJunitDiffPath(){
			return junitDiffPath;
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			System.out.println("config volan: " + this.getClass().getName());
			junitDiffPath = formData.getString("junitDiffPath");
			//TODO udelat nejaky check, ze je zapnute publikovani JUnitu
			save();
			return super.configure(req, formData);
		}
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
