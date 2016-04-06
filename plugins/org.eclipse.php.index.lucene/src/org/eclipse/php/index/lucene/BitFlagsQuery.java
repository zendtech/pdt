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

public class BitFlagsQuery extends Query {

	private final int fTrueFlags;
	private final int fFalseFlags;

	public BitFlagsQuery(int trueFlags, int falseFlags) {
		fTrueFlags = trueFlags;
		fFalseFlags = falseFlags;
	}

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
				final Scorer scorer = scorer(context);
				final boolean match = (scorer != null && scorer.iterator().advance(doc) == doc);
				if (match) {
					assert scorer.score() == 0;
					return Explanation.match(0, Messages.BitFlagsQuery_MatchOnId + doc);
				} else {
					return Explanation.match(0, Messages.BitFlagsQuery_NoMatchOnId + doc);
				}
			}
			@Override
			public Scorer scorer(LeafReaderContext context) throws IOException {
				final Bits acceptDocs = context.reader().getLiveDocs();
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
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + fFalseFlags;
		result = prime * result + fTrueFlags;
		return result;
	}

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

	@Override
	public String toString(String input) {
		return MessageFormat.format(Messages.BitFlagsQuery_BitFlagsQueryDescription, IndexFields.NDV_FLAGS, fTrueFlags,
				fFalseFlags);
	}

}
