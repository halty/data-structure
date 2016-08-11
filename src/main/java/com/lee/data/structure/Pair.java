package com.lee.data.structure;

public final class Pair<L, R> {

	private L left;
	private R right;
	
	public static <L, R> Pair<L, R> of(L left, R right) {
		return new Pair<L, R>(left, right);
	}
	
	private Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public R getRight() {
		return right;
	}

	public void setRight(R right) {
		this.right = right;
	}
	
	@Override
	public int hashCode() {
		int h = 0;
		if(left != null) {
			h += left.hashCode();
		}
		if(right != null) {
			h = 31*h + right.hashCode();
		}
		return h;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this) { return true; }
		if(obj instanceof Pair) {
			@SuppressWarnings("rawtypes")
			Pair other = (Pair) obj;
			
			if(left == null) {
				if(other.left != null) { return false; }
			}else {
				if(!left.equals(other.left)) { return false; }
			}
			if(right == null) {
				if(other.right != null) { return false; }
			}else {
				if(!right.equals(other.right)) { return false; }
			}
			return true;
		}else {
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(32);
		builder.append("Pair [left=").append(left).append(", right=")
				.append(right).append("]");
		return builder.toString();
	}
}
