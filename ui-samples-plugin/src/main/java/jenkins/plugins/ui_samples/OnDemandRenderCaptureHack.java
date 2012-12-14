package jenkins.plugins.ui_samples;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class OnDemandRenderCaptureHack extends UISample {
    @Override
    public String getDescription() {
        return "Capture additional variables for on-demand lazy rendering";
    }

    public List<Fruit> getFruits() {
        return Arrays.asList(new Apple(),new Orange(),new Apple());
    }

    public List<SourceFile> getSourceFiles() {
        return Arrays.asList(new SourceFile("index.groovy"));
    }

    public String getCapture1() {
        return String.valueOf(new Random().nextInt());
    }

    @Extension
    public static class DescriptorImpl extends UISampleDescriptor {
    }



    public static abstract class Fruit extends AbstractDescribableImpl<Fruit> implements ExtensionPoint {
    }

    public static class Apple extends Fruit {
        @Extension
        public static class DescriptorImpl extends Descriptor<Fruit> {
            @Override
            public String getDisplayName() {
                return "Apple";
            }
        }
    }

    public static class Orange extends Fruit {
        @Extension
        public static class DescriptorImpl extends Descriptor<Fruit> {
            @Override
            public String getDisplayName() {
                return "Orange";
            }
        }
    }
}

