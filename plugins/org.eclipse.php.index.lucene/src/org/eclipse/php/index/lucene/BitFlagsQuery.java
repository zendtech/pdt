/*******************************************************************************
 * Copyright (c) 2016 Zend Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.index.lucene;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Set;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreScorer;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocValuesDocIdSet;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;
import org.eclipse.dltk.ast.Modifiers;

/**
 * Query for scoring declaration modifiers represented by corresponding DLTK's
 * modifiers bit flags {@link Modifiers}.
 * 
 * @author Michal Niewrzal, Bartlomiej Laczkowski
 */
public class BitFlagsQuery extends Query {

	private final int fTrueFlags;
	private final int fFalseFlags;

	public BitFlagsQuery(final int trueFlags, final int falseFlags) {
		fTrueFlags = trueFlags;
		fFalseFlags = falseFlags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Query#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + fFalseFlags;
		result = prime * result + fTrueFlags;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Query#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		BitFlagsQuery other = (BitFlagsQuery) obj;
		if (fFalseFlags != other.fFalseFlags)
			return false;
		if (fTrueFlags != other.fTrueFlags)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Query#toString(java.lang.String)
	 */
	@Override
	public String toString(String input) {
		return MessageFormat.format(Messages.BitFlagsQuery_BitFlagsQueryDescription, IndexFields.NDV_FLAGS, fTrueFlags,
				fFalseFlags);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.
	 * IndexSearcher, boolean)
	 */
	@Override
	public Weight createWeight(IndexSearcher searcher, boolean needsScores) throws IOException {
		return new Weight(this) {
			@Override
			public void extractTerms(Set<Term> terms) {
				// Ignore
			}

			@Override
			public void normalize(float norm, float topLevelBoost) {
				// Ignore
			}

			@Override
			public float getValueForNormalization() throws IOException {
				return 0;
			}

			@Override
			public Explanation explain(LeafReaderContext context, int doc) throws IOException {
				final Scorer scorer = scorer(context, context.reader().getLiveDocs());
				final boolean match = (scorer != null && scorer.advance(doc) == doc);
				if (match) {
					assert scorer.score() == 0;
					return Explanation.match(0, Messages.BitFlagsQuery_MatchOnId + doc);
				} else {
					return Explanation.match(0, Messages.BitFlagsQuery_NoMatchOnId + doc);
				}
			}

			@Override
			public Scorer scorer(LeafReaderContext context, Bits acceptDocs) throws IOException {
				final DocIdSet set = getDocIdSet(context, acceptDocs);
				if (set == null) {
					return null;
				}
				final DocIdSetIterator iterator = set.iterator();
				if (iterator == null) {
					return null;
				}
				return new ConstantScoreScorer(this, 0, iterator);
			}
		};

	}

	/**
	 * Finds and returns matching doc ID set.
	 * 
	 * @param context
	 * @param acceptDocs
	 * @return matching doc ID set
	 * @throws IOException
	 */
	protected DocIdSet getDocIdSet(final LeafReaderContext context, Bits acceptDocs) throws IOException {
		final NumericDocValues numDocValues = DocValues.getNumeric(context.reader(), IndexFields.NDV_FLAGS);
		return new DocValuesDocIdSet(context.reader().maxDoc(), acceptDocs) {
			@Override
			protected boolean matchDoc(int doc) {
				long flags = numDocValues.get(doc);
				if (fTrueFlags != 0) {
					if ((flags & fTrueFlags) == 0) {
						return false;
					}
				}
				if (fFalseFlags != 0) {
					if ((flags & fFalseFlags) != 0) {
						return false;
					}
				}
				return true;
			}
		};
	}

}
