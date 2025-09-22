// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.justice.usermanager.db.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TblBpInfoExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    protected int startRow;

    protected int pageSize;

    public int getStartRow()
    {
        return startRow;
    }

    public void setStartRow(int startRow)
    {
        this.startRow = startRow;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }


    public TblBpInfoExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andBpIdIsNull() {
            addCriterion("bp_id is null");
            return (Criteria) this;
        }

        public Criteria andBpIdIsNotNull() {
            addCriterion("bp_id is not null");
            return (Criteria) this;
        }

        public Criteria andBpIdEqualTo(String value) {
            addCriterion("bp_id =", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdNotEqualTo(String value) {
            addCriterion("bp_id <>", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdGreaterThan(String value) {
            addCriterion("bp_id >", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdGreaterThanOrEqualTo(String value) {
            addCriterion("bp_id >=", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdLessThan(String value) {
            addCriterion("bp_id <", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdLessThanOrEqualTo(String value) {
            addCriterion("bp_id <=", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdLike(String value) {
            addCriterion("bp_id like", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdNotLike(String value) {
            addCriterion("bp_id not like", value, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdIn(List<String> values) {
            addCriterion("bp_id in", values, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdNotIn(List<String> values) {
            addCriterion("bp_id not in", values, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdBetween(String value1, String value2) {
            addCriterion("bp_id between", value1, value2, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpIdNotBetween(String value1, String value2) {
            addCriterion("bp_id not between", value1, value2, "bpId");
            return (Criteria) this;
        }

        public Criteria andBpNameIsNull() {
            addCriterion("bp_name is null");
            return (Criteria) this;
        }

        public Criteria andBpNameIsNotNull() {
            addCriterion("bp_name is not null");
            return (Criteria) this;
        }

        public Criteria andBpNameEqualTo(String value) {
            addCriterion("bp_name =", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameNotEqualTo(String value) {
            addCriterion("bp_name <>", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameGreaterThan(String value) {
            addCriterion("bp_name >", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameGreaterThanOrEqualTo(String value) {
            addCriterion("bp_name >=", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameLessThan(String value) {
            addCriterion("bp_name <", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameLessThanOrEqualTo(String value) {
            addCriterion("bp_name <=", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameLike(String value) {
            addCriterion("bp_name like", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameNotLike(String value) {
            addCriterion("bp_name not like", value, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameIn(List<String> values) {
            addCriterion("bp_name in", values, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameNotIn(List<String> values) {
            addCriterion("bp_name not in", values, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameBetween(String value1, String value2) {
            addCriterion("bp_name between", value1, value2, "bpName");
            return (Criteria) this;
        }

        public Criteria andBpNameNotBetween(String value1, String value2) {
            addCriterion("bp_name not between", value1, value2, "bpName");
            return (Criteria) this;
        }

        public Criteria andWebsiteIsNull() {
            addCriterion("website is null");
            return (Criteria) this;
        }

        public Criteria andWebsiteIsNotNull() {
            addCriterion("website is not null");
            return (Criteria) this;
        }

        public Criteria andWebsiteEqualTo(String value) {
            addCriterion("website =", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteNotEqualTo(String value) {
            addCriterion("website <>", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteGreaterThan(String value) {
            addCriterion("website >", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteGreaterThanOrEqualTo(String value) {
            addCriterion("website >=", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteLessThan(String value) {
            addCriterion("website <", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteLessThanOrEqualTo(String value) {
            addCriterion("website <=", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteLike(String value) {
            addCriterion("website like", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteNotLike(String value) {
            addCriterion("website not like", value, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteIn(List<String> values) {
            addCriterion("website in", values, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteNotIn(List<String> values) {
            addCriterion("website not in", values, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteBetween(String value1, String value2) {
            addCriterion("website between", value1, value2, "website");
            return (Criteria) this;
        }

        public Criteria andWebsiteNotBetween(String value1, String value2) {
            addCriterion("website not between", value1, value2, "website");
            return (Criteria) this;
        }

        public Criteria andLicenseIdIsNull() {
            addCriterion("license_id is null");
            return (Criteria) this;
        }

        public Criteria andLicenseIdIsNotNull() {
            addCriterion("license_id is not null");
            return (Criteria) this;
        }

        public Criteria andLicenseIdEqualTo(String value) {
            addCriterion("license_id =", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdNotEqualTo(String value) {
            addCriterion("license_id <>", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdGreaterThan(String value) {
            addCriterion("license_id >", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdGreaterThanOrEqualTo(String value) {
            addCriterion("license_id >=", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdLessThan(String value) {
            addCriterion("license_id <", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdLessThanOrEqualTo(String value) {
            addCriterion("license_id <=", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdLike(String value) {
            addCriterion("license_id like", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdNotLike(String value) {
            addCriterion("license_id not like", value, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdIn(List<String> values) {
            addCriterion("license_id in", values, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdNotIn(List<String> values) {
            addCriterion("license_id not in", values, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdBetween(String value1, String value2) {
            addCriterion("license_id between", value1, value2, "licenseId");
            return (Criteria) this;
        }

        public Criteria andLicenseIdNotBetween(String value1, String value2) {
            addCriterion("license_id not between", value1, value2, "licenseId");
            return (Criteria) this;
        }

        public Criteria andMasterUserIsNull() {
            addCriterion("master_user is null");
            return (Criteria) this;
        }

        public Criteria andMasterUserIsNotNull() {
            addCriterion("master_user is not null");
            return (Criteria) this;
        }

        public Criteria andMasterUserEqualTo(String value) {
            addCriterion("master_user =", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserNotEqualTo(String value) {
            addCriterion("master_user <>", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserGreaterThan(String value) {
            addCriterion("master_user >", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserGreaterThanOrEqualTo(String value) {
            addCriterion("master_user >=", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserLessThan(String value) {
            addCriterion("master_user <", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserLessThanOrEqualTo(String value) {
            addCriterion("master_user <=", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserLike(String value) {
            addCriterion("master_user like", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserNotLike(String value) {
            addCriterion("master_user not like", value, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserIn(List<String> values) {
            addCriterion("master_user in", values, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserNotIn(List<String> values) {
            addCriterion("master_user not in", values, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserBetween(String value1, String value2) {
            addCriterion("master_user between", value1, value2, "masterUser");
            return (Criteria) this;
        }

        public Criteria andMasterUserNotBetween(String value1, String value2) {
            addCriterion("master_user not between", value1, value2, "masterUser");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("status is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("status is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(Integer value) {
            addCriterion("status =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(Integer value) {
            addCriterion("status <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(Integer value) {
            addCriterion("status >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(Integer value) {
            addCriterion("status >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(Integer value) {
            addCriterion("status <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(Integer value) {
            addCriterion("status <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<Integer> values) {
            addCriterion("status in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<Integer> values) {
            addCriterion("status not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(Integer value1, Integer value2) {
            addCriterion("status between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(Integer value1, Integer value2) {
            addCriterion("status not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andContactInfoIsNull() {
            addCriterion("contact_info is null");
            return (Criteria) this;
        }

        public Criteria andContactInfoIsNotNull() {
            addCriterion("contact_info is not null");
            return (Criteria) this;
        }

        public Criteria andContactInfoEqualTo(String value) {
            addCriterion("contact_info =", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoNotEqualTo(String value) {
            addCriterion("contact_info <>", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoGreaterThan(String value) {
            addCriterion("contact_info >", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoGreaterThanOrEqualTo(String value) {
            addCriterion("contact_info >=", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoLessThan(String value) {
            addCriterion("contact_info <", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoLessThanOrEqualTo(String value) {
            addCriterion("contact_info <=", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoLike(String value) {
            addCriterion("contact_info like", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoNotLike(String value) {
            addCriterion("contact_info not like", value, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoIn(List<String> values) {
            addCriterion("contact_info in", values, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoNotIn(List<String> values) {
            addCriterion("contact_info not in", values, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoBetween(String value1, String value2) {
            addCriterion("contact_info between", value1, value2, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andContactInfoNotBetween(String value1, String value2) {
            addCriterion("contact_info not between", value1, value2, "contactInfo");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return (Criteria) this;
        }

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIsNull() {
            addCriterion("update_time is null");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIsNotNull() {
            addCriterion("update_time is not null");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeEqualTo(Date value) {
            addCriterion("update_time =", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotEqualTo(Date value) {
            addCriterion("update_time <>", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThan(Date value) {
            addCriterion("update_time >", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("update_time >=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThan(Date value) {
            addCriterion("update_time <", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeLessThanOrEqualTo(Date value) {
            addCriterion("update_time <=", value, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeIn(List<Date> values) {
            addCriterion("update_time in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotIn(List<Date> values) {
            addCriterion("update_time not in", values, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeBetween(Date value1, Date value2) {
            addCriterion("update_time between", value1, value2, "updateTime");
            return (Criteria) this;
        }

        public Criteria andUpdateTimeNotBetween(Date value1, Date value2) {
            addCriterion("update_time not between", value1, value2, "updateTime");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}
