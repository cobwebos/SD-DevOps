import antlr.ANTLRException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scheduler.CronTab;
import hudson.scheduler.CronTabList;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;

import java.util.Calendar;

/**
 * @author Kohsuke Kawaguchi
 */
public class FooTest {
    public static void main(String[] args) throws ANTLRException {

        Jenkins j = Jenkins.getInstance();
        for (AbstractProject p : j.getAllItems(AbstractProject.class)) {
            p.getDisplayName();
            AbstractBuild b = p.getLastBuild();
            if (b!=null) {
                b.getTimestamp();
                if (b.isBuilding()) {
                    //
                }
            }

            p.getLastCompletedBuild();

            TimerTrigger tt = (TimerTrigger) p.getTrigger(TimerTrigger.class);
            if (tt!=null) {
                CronTabList tab = CronTabList.create(tt.getSpec());
                for (CronTab cronTab : tab) {
                    Calendar t = cronTab.ceil(System.currentTimeMillis());
                }
            }
        }
    }
}
