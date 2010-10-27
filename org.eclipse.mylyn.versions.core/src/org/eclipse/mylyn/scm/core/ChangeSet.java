/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.scm.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Steffen Pingel
 */
public class ChangeSet {

	List<ScmArtifact> artifacts = new ArrayList<ScmArtifact>();

	ScmUser author;

	Date date;

	/**
	 * SHA1 hash or revision.
	 */
	String id;

	String kind;

	String message;

	ScmRepository repository;

	public List<ScmArtifact> getArtifacts() {
		return artifacts;
	}

	public String getKind() {
		return kind;
	}

	public ScmRepository getRepository() {
		return repository;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public void setRepository(ScmRepository repository) {
		this.repository = repository;
	}

}
