// Copyright (C) 2008 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.sonatype.sshjgit.core.gitcommand;

import org.apache.shiro.SecurityUtils;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;

import java.io.File;
import java.io.IOException;

abstract class AbstractGitCommand extends AbstractCommand {
    protected final File reposRootDir;
    protected final String reposRootDirPath;
    protected Repository repo;
    private static final String VALID_PROJECTNAME_REGEX =
            "[a-zA-Z0-9_][a-zA-Z0-9_.-]*(/[a-zA-Z0-9_][a-zA-Z0-9_.-]*)*";

    public AbstractGitCommand(File reposRootDir) {
        this.reposRootDir = reposRootDir;
        this.reposRootDirPath = reposRootDir.getAbsolutePath();
    }

    /**
     * Extracts the repo's directory path relative to the repo root, and
     * converts slashes to colon, so that subdirectories become permission parts
     * @param repo the git repo
     * @return a colon separated string of directories, representing the repo's
     * location in the repo root directory.
     */
    protected String getRepoNameAsPermissionParts(Repository repo) {
        final String repoPath = repo.getDirectory().getAbsolutePath();
        String relativePath = repoPath.replace(reposRootDirPath, "");
        if (relativePath.startsWith("/")){
            relativePath = relativePath.substring(1);
        }
        return relativePath.replace('/', ':');
    }

    @Override
    protected final void run( String[] args ) throws IOException, Failure {
        String projectName = parseCommandLine( args );
        if ( projectName.endsWith( ".git" ) ) {
            // Be nice and drop the trailing ".git" suffix, which we never keep
            // in our database, but clients might mistakenly provide anyway.
            //
            projectName = projectName.substring( 0, projectName.length() - 4 );
        }
        if ( projectName.startsWith( "/" ) ) {
            // Be nice and drop the leading "/" if supplied by an absolute path.
            // We don't have a file system hierarchy, just a flat namespace in
            // the database's Project entities. We never encode these with a
            // leading '/' but users might accidentally include them in Git URLs.
            //
            projectName = projectName.substring( 1 );
        }
        if (!projectName.matches(VALID_PROJECTNAME_REGEX)){
            // Disallow dangerous project names which for example attempt to
            // traverse directories backwards with ../../
            // Unicode attacks is another example of things we filter out.
            // Only way to safely avoid all these (and future) attacks, are
            // by only allowing a known safe set of names.  
            throw new Failure(2, "Disallowed project name. Valid project " +
                    "names are for example 'project1', 'subdir/project2' and " +
                    "'subdir/subsubdir/project3'.");
        }

        // TODO: Should we have a locking mechanism, so no two clients can work with the same repo at the same time? Is it handled by JGit internally already?
        repo = new Repository( new File( projectName ) );
        final RepositoryConfig repositoryConfig = repo.getConfig();
        if (!repositoryConfig.getFile().exists()) {
            // TODO: Check so any of the parent directories in the path leading up to this location, doesn't already contain a repo.
            SecurityUtils.getSubject().checkPermission("gitrepo:new:" + getRepoNameAsPermissionParts(repo));
            repo.create();
        }

        try {
            runImpl();
        } finally {
            repo.close();
        }
    }

    protected abstract void runImpl() throws IOException, Failure;

    protected abstract String parseCommandLine( String[] args ) throws Failure;
}