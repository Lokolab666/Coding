Look my terraform.tfvars

  {
    name            = "AWSManagedRulesCommonRuleSet-EC2MetaDataSSRF_QUERYARGUMENTS"
    label_match_key = "awswaf:managed:aws:core-rule-set:EC2MetaDataSSRF_QUERYARGUMENTS"
    exceptions = [
      {
        match_string  = "/registration/client/"
        match_pattern = "STARTS_WITH"
      }
    ]
  },
  {
    name            = "AWSManagedRulesCommonRuleSet-EC2MetaDataSSRF_BODY"
    label_match_key = "awswaf:managed:aws:core-rule-set:EC2MetaDataSSRF_BODY"
    exceptions = [
      {
        match_string  = "/registration/"
        match_pattern = "STARTS_WITH"
      }
    ]
  },