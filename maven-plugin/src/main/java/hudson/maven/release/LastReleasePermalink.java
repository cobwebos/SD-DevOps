/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.maven.release;

import hudson.model.Job;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Result;
import hudson.model.Run;

/**
 * Resolves to the last release.
 *
 * @author Kohsuke Kawaguchi
 */
public class LastReleasePermalink extends Permalink {
    public static final Permalink INSTANCE = new LastReleasePermalink();

    @Override
    public String getDisplayName() {
        return "Last Release";
    }

    @Override
    public String getId() {
        return "lastRelease";
    }

    @Override
    public Run<?,?> resolve(Job<?, ?> job) {
        for (Run<?,?> run: job.getBuilds()){
            MvnReleaseBadgeAction a = run.getAction(MvnReleaseBadgeAction.class);
            if (a!=null && run.getResult()== Result.SUCCESS)
                return run;
        }
        return null;
    }
}
